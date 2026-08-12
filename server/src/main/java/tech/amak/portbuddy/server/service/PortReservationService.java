/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.server.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.PortReservationRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortReservationService {

    private static final int MAX_RETRIES = 10;

    private final PortReservationRepository repository;
    private final ProxyDiscoveryService proxyDiscoveryService;
    private final TunnelRepository tunnelRepository;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public List<PortReservationEntity> getReservations(final AccountEntity account) {
        return repository.findAllByAccount(account);
    }

    /**
     * Attempts to reserve a unique (publicHost, publicPort) pair for the given account following rules:
     * - Discover available tcp-proxy public hosts and select the host with the least number of reservations.
     * - Port assignments are incremental per host within configurable range [min,max].
     * - If next port for the selected host is out of range, try the next host.
     * - If no combination can be generated, throw an exception.
     * Uniqueness is enforced by a DB unique constraint; in case of race conflicts, the operation retries.
     */
    @Transactional
    public Optional<PortReservationEntity> createReservation(final AccountEntity account,
                                                             final UserEntity user) {
        final var hosts = proxyDiscoveryService.listPublicHosts();
        if (hosts.isEmpty()) {
            log.warn("No available tcp-proxy hosts found");
            return Optional.empty();
        }

        int attempts = 0;
        while (attempts++ < MAX_RETRIES) {
            // Order hosts by least reservations
            final var orderedHosts = hosts.stream()
                .sorted(Comparator.comparingLong(repository::countByPublicHost))
                .toList();

            for (final String host : orderedHosts) {
                final var range = portRangeFor(host);
                final int min = range.min();
                final int max = range.max();
                if (min <= 0 || max <= 0 || min > max) {
                    throw new IllegalStateException("Invalid port range configuration: [" + min + ", " + max + "]");
                }
                final var nextPort = computeNextPort(host, min, max);
                if (nextPort == null) {
                    // This host is exhausted, try next
                    continue;
                }

                try {
                    final var reservation = new PortReservationEntity();
                    reservation.setId(UUID.randomUUID());
                    reservation.setAccount(account);
                    reservation.setUser(user);
                    reservation.setPublicHost(host);
                    reservation.setPublicPort(nextPort);
                    final var saved = repository.save(reservation);
                    log.info("Reserved port {}:{} for account {}", host, nextPort, account.getId());
                    return Optional.of(saved);
                } catch (final DataIntegrityViolationException e) {
                    // Unique constraint violation possible due to race; retry
                    log.warn("Port reservation conflict for {}:{}, will retry (attempt {}/{})",
                        host, nextPort, attempts, MAX_RETRIES);
                }
            }

            // If we got here, we either had conflicts on all hosts or all were exhausted; retry loop continues
        }

        log.warn("Failed to reserve a unique port after " + MAX_RETRIES + " attempts");
        return Optional.empty();
    }

    private Integer computeNextPort(final String host, final int min, final int max) {
        // Efficiently find the minimal available port via a single DB query.
        return repository.findMinimalFreePort(host, min, max).orElse(null);
    }

    /**
     * Deletes a reservation associated with the specified account.
     *
     * @param id      the unique identifier of the reservation to delete
     * @param account the account entity associated with the reservation
     */
    @Transactional
    public void deleteReservation(final UUID id, final AccountEntity account) {
        final var entity = repository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
        if (isReservationInUse(entity)) {
            throw new IllegalStateException("Reservation is in use by active tunnels");
        }
        repository.delete(entity);
    }

    /**
     * Resolve a port reservation for a NET (TCP/UDP) expose request according to rules:
     * - If explicit reservation (host:port or port) provided, ensure it belongs to the account and is not used by
     * any active tunnel. If multiple reservations found for the same port, take the first one.
     * - Otherwise, if there was a previous tunnel for the same local resource that used a reservation
     * and it's free, reuse it.
     * - Otherwise, pick the first existing reservation of the account that is not in use by any active tunnel.
     * - If none exist, create a new reservation and return it.
     */
    @Transactional
    public PortReservationEntity resolveForNetExpose(final AccountEntity account,
                                                     final UserEntity user,
                                                     final String localHost,
                                                     final int localPort,
                                                     final String explicitHostPort) {
        // 1) Explicit reservation
        if (explicitHostPort != null && !explicitHostPort.isBlank()) {
            final var hostPort = explicitHostPort.trim();
            final int colon = hostPort.lastIndexOf(':');
            final PortReservationEntity reservation;
            if (colon > 0 && colon < hostPort.length() - 1) {
                // public_host:port format
                final var host = hostPort.substring(0, colon);
                final var port = Integer.parseInt(hostPort.substring(colon + 1));
                reservation = repository.findByAccountAndPublicHostAndPublicPort(account, host, port)
                    .orElseThrow(() ->
                        new IllegalArgumentException("Port reservation not found for this account: " + hostPort));
            } else if (colon == -1) {
                // Try as name first (lookup by account and port reservation name case insensitive)
                reservation = repository.findByAccountAndNameIgnoreCase(account, hostPort)
                    .or(() -> {
                        try {
                            final var port = Integer.parseInt(hostPort);
                            final var reservations = repository.findAllByAccountAndPublicPort(account, port);
                            if (reservations.isEmpty()) {
                                throw new IllegalArgumentException(
                                    "Port reservation not found for this account and port: " + port);
                            }
                            return Optional.of(reservations.getFirst());
                        } catch (final NumberFormatException e) {
                            throw new IllegalArgumentException(
                                "Port reservation name or port not found for this account: " + hostPort);
                        }
                    })
                    .orElseThrow(() ->
                        new IllegalArgumentException("Port reservation not found for this account: " + hostPort));

            } else {
                throw new IllegalArgumentException("Invalid --port-reservation value, expected host:port or port");
            }

            if (isReservationInUse(reservation)) {
                throw new IllegalStateException("Port reservation is currently in use: " + hostPort);
            }
            return reservation;
        }

        // 2) Reuse by same local resource if possible
        final var prev = tunnelRepository
            .findFirstByAccountIdAndLocalHostAndLocalPortAndPortReservationIsNotNullOrderByCreatedAtDesc(
                account.getId(), localHost, localPort);
        if (prev.isPresent()) {
            final var res = prev.get().getPortReservation();
            if (res != null && !res.isDeleted() && !isReservationInUse(res)) {
                return res;
            }
        }

        // 3) First available among existing reservations
        final var existing = repository.findAllByAccount(account).stream()
            .sorted(Comparator.comparing(PortReservationEntity::getCreatedAt))
            .filter(res -> !isReservationInUse(res))
            .findFirst();

        // 4) Create new reservation
        return existing.orElseGet(() -> createReservation(account, user)
            .orElseThrow(() -> new IllegalStateException("Failed to create a new port reservation")));
    }

    private boolean isReservationInUse(final PortReservationEntity reservation) {
        return tunnelRepository.existsByPortReservationAndStatusNot(reservation, TunnelStatus.CLOSED);
    }

    /**
     * Updates an existing reservation host/port ensuring constraints.
     */
    @Transactional
    public PortReservationEntity updateReservation(final AccountEntity account,
                                                   final UUID id,
                                                   final String host,
                                                   final Integer port,
                                                   final String name) {
        final var entity = repository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
        if (isReservationInUse(entity)) {
            throw new IllegalStateException("Reservation is in use by active tunnels");
        }

        final var previousHost = entity.getPublicHost();
        final var previousPort = entity.getPublicPort();

        if (host != null) {
            if (proxyDiscoveryService.findByHost(host).isEmpty()) {
                throw new IllegalArgumentException("Unknown public host: " + host);
            }
            entity.setPublicHost(host);
        }

        if (port != null) {
            final var range = portRangeFor(entity.getPublicHost());
            if (port < range.min() || port > range.max()) {
                throw new IllegalArgumentException(
                    "Port must be between " + range.min() + " and " + range.max() + " on " + entity.getPublicHost());
            }
            entity.setPublicPort(port);
        }

        final var hostPortChanged = !Objects.equals(previousHost, entity.getPublicHost())
            || !Objects.equals(previousPort, entity.getPublicPort());
        if (hostPortChanged && repository.existsByPublicHostAndPublicPortAndIdNot(
            entity.getPublicHost(), entity.getPublicPort(), entity.getId())) {
            throw new PortAlreadyReservedException(entity.getPublicHost(), entity.getPublicPort());
        }

        if (name != null) {
            // A blank name means "no name" — clearing it must not collide with other unnamed reservations
            final var newName = name.isBlank() ? null : name.trim();
            if (!Objects.equals(newName, entity.getName())) {
                if (newName != null && repository.existsByAccountAndName(account, newName)) {
                    throw new IllegalArgumentException("Reservation with name '" + newName + "' already exists");
                }
                entity.setName(newName);
            }
        }

        try {
            // Trigger unique check on save
            return repository.saveAndFlush(entity);
        } catch (final DataIntegrityViolationException e) {
            // Lost a race against a concurrent reservation of the same host:port
            throw new PortAlreadyReservedException(entity.getPublicHost(), entity.getPublicPort());
        }
    }

    /**
     * Tells whether a public port on a given host can still be reserved, i.e. it is inside that proxy's
     * allowed range and not already taken by another (non-deleted) reservation.
     *
     * @param host      the proxy public host
     * @param port      the public port to check
     * @param excludeId reservation to ignore when checking for conflicts, typically the one being edited
     * @return true when the port may be reserved
     */
    @Transactional(readOnly = true)
    public boolean isPortAvailable(final String host, final int port, final UUID excludeId) {
        final var range = portRangeFor(host);
        if (port < range.min() || port > range.max()) {
            return false;
        }
        return excludeId != null
            ? !repository.existsByPublicHostAndPublicPortAndIdNot(host, port, excludeId)
            : !repository.existsByPublicHostAndPublicPort(host, port);
    }

    /**
     * Finds the lowest public port still free on a host, within that host's reservable range.
     *
     * @param host the proxy public host
     * @return the lowest free port, or empty when the host's range is exhausted
     */
    @Transactional(readOnly = true)
    public Optional<Integer> findNextFreePort(final String host) {
        final var range = portRangeFor(host);
        return repository.findMinimalFreePort(host, range.min(), range.max());
    }

    /**
     * Resolves the reservable port range of a proxy, falling back to the globally configured range when the
     * instance does not advertise one (or is not registered at all).
     *
     * @param host the proxy public host
     * @return the allowed port range for that host
     */
    public AppProperties.PortReservations.Range portRangeFor(final String host) {
        return proxyDiscoveryService.findByHost(host)
            .map(p -> new AppProperties.PortReservations.Range(p.portMin(), p.portMax()))
            .orElseGet(() -> properties.portReservations().range());
    }

    /**
     * Raised when the requested host:port pair is already reserved by someone else.
     */
    public static class PortAlreadyReservedException extends IllegalStateException {
        public PortAlreadyReservedException(final String host, final Integer port) {
            super("Port " + port + " on " + host + " is already reserved. Please choose another port.");
        }
    }
}
