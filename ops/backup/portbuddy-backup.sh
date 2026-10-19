#!/usr/bin/env bash
#
# Daily backup of Port Buddy host data to Google Drive.
#
# What is backed up:
#   - /root/app  (everything except /root/app/<service>/logs/)
#   - /root/mail
#   - a consistent logical dump of PostgreSQL (pg_dumpall from the pb-db container),
#     because raw pg_data files copied from a running database may be inconsistent.
#
# The archive is uploaded with rclone to "${RCLONE_REMOTE}:${RCLONE_DIR}".
# Settings can be overridden in /etc/portbuddy-backup.conf (sourced as bash).
# After every run an email report is sent through the docker-mailserver container
# ("${MAIL_CONTAINER}") to "${NOTIFY_EMAIL}".
#
# Usage:
#   portbuddy-backup.sh             # create archive and upload it
#   SKIP_UPLOAD=1 portbuddy-backup.sh   # create archive locally only (testing)

set -Eeuo pipefail

CONFIG_FILE="${CONFIG_FILE:-/etc/portbuddy-backup.conf}"
if [[ -f "${CONFIG_FILE}" ]]; then
  # shellcheck source=/dev/null
  source "${CONFIG_FILE}"
fi

# Source directories (absolute paths, space separated)
BACKUP_SOURCES="${BACKUP_SOURCES:-/root/app /root/mail}"
# Postgres container name (empty string disables the SQL dump)
DB_CONTAINER="${DB_CONTAINER-pb-db}"
# Where archives are staged locally
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-/var/backups/portbuddy}"
# How many days of archives to keep locally / on Google Drive (>= 1)
LOCAL_RETENTION_DAYS="${LOCAL_RETENTION_DAYS:-3}"
REMOTE_RETENTION_DAYS="${REMOTE_RETENTION_DAYS:-30}"
# rclone remote name (see `rclone config`) and target folder on Google Drive
RCLONE_REMOTE="${RCLONE_REMOTE:-gdrive}"
RCLONE_DIR="${RCLONE_DIR:-portbuddy-backups}"
RCLONE_CONFIG="${RCLONE_CONFIG:-/root/.config/rclone/rclone.conf}"
# Optional: file containing a passphrase; if set, the archive is GPG (AES256) encrypted
GPG_PASSPHRASE_FILE="${GPG_PASSPHRASE_FILE:-}"
SKIP_UPLOAD="${SKIP_UPLOAD:-0}"
LOCK_FILE="${LOCK_FILE:-/run/lock/portbuddy-backup.lock}"
# Email report: recipient (empty disables), sender, mail container, and when to send (always|failure)
NOTIFY_EMAIL="${NOTIFY_EMAIL-admin@portbuddy.dev}"
NOTIFY_FROM="${NOTIFY_FROM:-Port Buddy Backup <admin@portbuddy.dev>}"
MAIL_CONTAINER="${MAIL_CONTAINER:-dmail}"
NOTIFY_ON="${NOTIFY_ON:-always}"

export RCLONE_CONFIG

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

fail() {
  log "ERROR: $*" >&2
  exit 1
}

on_error() {
  log "ERROR: backup failed at line ${1} (exit code ${2})" >&2
}

run_backup() {
  set -Eeuo pipefail
  trap 'on_error "${LINENO}" "$?"' ERR

  # Prevent overlapping runs
  exec 9>"${LOCK_FILE}"
  flock -n 9 || fail "another backup is already running"

  for cmd in tar zstd flock; do
    command -v "${cmd}" >/dev/null || fail "required command not found: ${cmd}"
  done
  if [[ "${SKIP_UPLOAD}" != "1" ]]; then
    command -v rclone >/dev/null || fail "rclone is not installed"
    rclone listremotes | grep -qx "${RCLONE_REMOTE}:" \
      || fail "rclone remote '${RCLONE_REMOTE}' is not configured (run: rclone config)"
  fi

  TIMESTAMP="$(date '+%Y-%m-%d_%H-%M-%S')"
  ARCHIVE_NAME="portbuddy-${TIMESTAMP}.tar.zst"

  mkdir -p "${LOCAL_BACKUP_DIR}"
  chmod 700 "${LOCAL_BACKUP_DIR}"
  umask 077

  WORK_DIR="$(mktemp -d "${LOCAL_BACKUP_DIR}/.work.XXXXXX")"
  cleanup() {
    rm -rf "${WORK_DIR}"
  }
  trap cleanup EXIT

  log "Starting backup ${ARCHIVE_NAME}"

  # 1. Consistent database dump
  if [[ -n "${DB_CONTAINER}" ]]; then
    if docker ps --format '{{.Names}}' | grep -qx "${DB_CONTAINER}"; then
      log "Dumping PostgreSQL from container ${DB_CONTAINER}"
      docker exec "${DB_CONTAINER}" sh -c 'pg_dumpall -U "$POSTGRES_USER" --clean --if-exists' \
        | zstd -q -T0 -o "${WORK_DIR}/postgres-dumpall.sql.zst"
    else
      log "WARNING: container ${DB_CONTAINER} is not running, skipping SQL dump"
    fi
  fi

  # 2. Archive files. Paths are stored relative to "/", e.g. "root/app/...".
  TAR_SOURCES=()
  for src in ${BACKUP_SOURCES}; do
    if [[ -e "${src}" ]]; then
      TAR_SOURCES+=("${src#/}")
    else
      log "WARNING: source ${src} does not exist, skipping"
    fi
  done
  [[ ${#TAR_SOURCES[@]} -gt 0 ]] || fail "nothing to back up"

  EXTRA_ARGS=()
  if [[ -f "${WORK_DIR}/postgres-dumpall.sql.zst" ]]; then
    EXTRA_ARGS=(--directory="${WORK_DIR}" postgres-dumpall.sql.zst)
  fi

  ARCHIVE_PATH="${LOCAL_BACKUP_DIR}/${ARCHIVE_NAME}"
  log "Archiving: ${TAR_SOURCES[*]} (excluding root/app/*/logs)"

  set +e
  trap - ERR
  tar --create \
      --file=- \
      --warning=no-file-changed \
      --warning=no-file-removed \
      --warning=no-file-ignored \
      --no-wildcards-match-slash \
      --exclude='root/app/*/logs' \
      --directory=/ "${TAR_SOURCES[@]}" \
      "${EXTRA_ARGS[@]}" \
    | zstd -q -T0 -10 -o "${ARCHIVE_PATH}.partial"
  pipe_status=("${PIPESTATUS[@]}")
  trap 'on_error "${LINENO}" "$?"' ERR
  set -e
  # tar exit code 1 means "some files changed while being read" (e.g. live pg_data / logs) - acceptable
  if [[ ${pipe_status[0]} -gt 1 || ${pipe_status[1]} -ne 0 ]]; then
    rm -f "${ARCHIVE_PATH}.partial"
    fail "archive creation failed (tar=${pipe_status[0]}, zstd=${pipe_status[1]})"
  fi
  mv "${ARCHIVE_PATH}.partial" "${ARCHIVE_PATH}"
  zstd -q -t "${ARCHIVE_PATH}" || fail "archive integrity check failed"

  # 3. Optional encryption
  if [[ -n "${GPG_PASSPHRASE_FILE}" ]]; then
    [[ -r "${GPG_PASSPHRASE_FILE}" ]] || fail "cannot read GPG passphrase file ${GPG_PASSPHRASE_FILE}"
    log "Encrypting archive with GPG (AES256)"
    gpg --batch --yes --quiet --symmetric --cipher-algo AES256 \
        --pinentry-mode loopback --passphrase-file "${GPG_PASSPHRASE_FILE}" \
        --output "${ARCHIVE_PATH}.gpg" "${ARCHIVE_PATH}"
    rm -f "${ARCHIVE_PATH}"
    ARCHIVE_PATH="${ARCHIVE_PATH}.gpg"
  fi

  log "Archive created: ${ARCHIVE_PATH} ($(du -h "${ARCHIVE_PATH}" | cut -f1))"

  # 4. Upload to Google Drive and apply remote retention
  if [[ "${SKIP_UPLOAD}" == "1" ]]; then
    log "SKIP_UPLOAD=1, not uploading"
  else
    REMOTE_PATH="${RCLONE_REMOTE}:${RCLONE_DIR}"
    log "Uploading to ${REMOTE_PATH}"
    rclone copy "${ARCHIVE_PATH}" "${REMOTE_PATH}" \
      --retries 5 --low-level-retries 20 --stats-one-line --stats 0
    log "Removing remote backups older than ${REMOTE_RETENTION_DAYS} days"
    # --drive-use-trash=false deletes old backups permanently (frees Drive quota)
    # without touching anything else in the Drive trash
    rclone delete "${REMOTE_PATH}" --min-age "${REMOTE_RETENTION_DAYS}d" \
      --include 'portbuddy-*' --drive-use-trash=false
  fi

  # 5. Local retention
  find "${LOCAL_BACKUP_DIR}" -maxdepth 1 -type f -name 'portbuddy-*' \
    -mtime +"$((LOCAL_RETENTION_DAYS - 1))" -print -delete | sed 's/^/Removed local: /'

  log "Backup finished successfully"
}

send_report() {
  local status="$1" run_log="$2" duration="$3"
  local mail_host envelope_from
  mail_host="$(hostname -f 2>/dev/null || hostname)"
  # envelope sender = bare address from NOTIFY_FROM
  envelope_from="$(sed -E 's/.*<([^>]+)>.*/\1/' <<<"${NOTIFY_FROM}")"

  if ! docker ps --format '{{.Names}}' | grep -qx "${MAIL_CONTAINER}"; then
    log "WARNING: mail container ${MAIL_CONTAINER} is not running, report not sent" >&2
    return 1
  fi

  {
    echo "From: ${NOTIFY_FROM}"
    echo "To: ${NOTIFY_EMAIL}"
    echo "Subject: [Port Buddy] Backup ${status} on ${mail_host} ($(date '+%Y-%m-%d'))"
    echo "Date: $(date -R)"
    echo "MIME-Version: 1.0"
    echo "Content-Type: text/plain; charset=UTF-8"
    echo "Content-Transfer-Encoding: 8bit"
    echo
    echo "Status:   ${status}"
    echo "Host:     ${mail_host}"
    echo "Duration: ${duration}s"
    echo "Target:   ${RCLONE_REMOTE}:${RCLONE_DIR}"
    echo "Local:    ${LOCAL_BACKUP_DIR} ($(df -h --output=avail "${LOCAL_BACKUP_DIR}" 2>/dev/null | tail -1 | tr -d ' ') free)"
    echo
    echo "---- log (last 200 lines) ----"
    tail -n 200 "${run_log}"
  } | docker exec -i "${MAIL_CONTAINER}" sendmail -t -i -f "${envelope_from}"
}

RUN_LOG="$(mktemp /tmp/portbuddy-backup-log.XXXXXX)"
trap 'rm -f "${RUN_LOG}"' EXIT
START_TS="$(date +%s)"

# Run in a subshell so that its "exit" calls do not skip the report below
set +e
( run_backup ) 2>&1 | tee "${RUN_LOG}"
rc="${PIPESTATUS[0]}"
set -e

if [[ "${rc}" -eq 0 ]]; then STATUS="OK"; else STATUS="FAILED (exit code ${rc})"; fi

if [[ -n "${NOTIFY_EMAIL}" && ( "${NOTIFY_ON}" == "always" || "${rc}" -ne 0 ) ]]; then
  if send_report "${STATUS}" "${RUN_LOG}" "$(( $(date +%s) - START_TS ))"; then
    log "Report sent to ${NOTIFY_EMAIL}"
  else
    log "WARNING: failed to send report to ${NOTIFY_EMAIL}" >&2
  fi
fi

exit "${rc}"
