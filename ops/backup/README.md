# Port Buddy daily backup to Google Drive

`portbuddy-backup.sh` creates a daily `portbuddy-YYYY-MM-DD_HH-MM-SS.tar.zst` archive containing:

- `/root/app` — everything except `/root/app/<service>/logs/`
- `/root/mail`
- `postgres-dumpall.sql.zst` — a consistent `pg_dumpall` of the `pb-db` container (raw `pg_data` files copied
  from a running Postgres may be inconsistent, so restore from the dump)

The archive is uploaded with [rclone](https://rclone.org/drive/) to `gdrive:portbuddy-backups`.
Backups older than 30 days are removed from Drive, older than 3 days are removed locally (`/var/backups/portbuddy`).

After every run an email report (`[Port Buddy] Backup OK ...` / `Backup FAILED ...`, with the run log) is sent
to `admin@portbuddy.dev` via `sendmail` inside the docker-mailserver container `dmail`.

## Install (Ubuntu 24.04, as root)

```bash
apt-get install -y rclone zstd
install -m 755 portbuddy-backup.sh /usr/local/bin/portbuddy-backup.sh
cp portbuddy-backup.service portbuddy-backup.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now portbuddy-backup.timer   # runs daily at ~03:30 server time
```

## Connect Google Drive (one-time)

The server has no browser, so the OAuth token is obtained on your laptop:

1. On your laptop: `brew install rclone` then `rclone authorize "drive"` — log in to Google in the browser and
   copy the printed token JSON.
2. On the server: `rclone config` → `n` (new remote) → name `gdrive` → storage `drive` → leave client id/secret
   empty → scope `1` (full access) or `3` (`drive.file`, only files created by rclone) → leave the rest default →
   "Use web browser to automatically authenticate?" `n` → paste the token → not a shared drive → `y`.
3. Check: `rclone lsd gdrive:`

## Configuration

Optional overrides in `/etc/portbuddy-backup.conf` (sourced as bash), e.g.:

```bash
RCLONE_REMOTE=gdrive
RCLONE_DIR=portbuddy-backups
REMOTE_RETENTION_DAYS=30
LOCAL_RETENTION_DAYS=3
# Encrypt archives before upload (recommended: they contain .env secrets, JWT keys and mail)
GPG_PASSPHRASE_FILE=/root/.backup-passphrase
# Email report (NOTIFY_EMAIL= disables it; NOTIFY_ON=failure sends only on errors)
NOTIFY_EMAIL=admin@portbuddy.dev
NOTIFY_FROM="Port Buddy Backup <admin@portbuddy.dev>"
MAIL_CONTAINER=dmail
NOTIFY_ON=always
```

## Operate

```bash
systemctl start portbuddy-backup.service        # run now
journalctl -u portbuddy-backup.service -n 50    # logs
systemctl list-timers portbuddy-backup.timer    # next run
SKIP_UPLOAD=1 portbuddy-backup.sh               # local-only test run
```

## Restore

```bash
rclone copy gdrive:portbuddy-backups/portbuddy-<date>.tar.zst .
# if encrypted: gpg --decrypt --output backup.tar.zst portbuddy-<date>.tar.zst.gpg
mkdir restore && tar --zstd -xpf portbuddy-<date>.tar.zst -C restore
# files are under restore/root/app and restore/root/mail
# database: start a fresh pb-db, then
zstd -dc restore/postgres-dumpall.sql.zst | docker exec -i pb-db sh -c 'psql -U "$POSTGRES_USER" -d postgres'
```
