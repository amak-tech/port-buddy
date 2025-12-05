/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.util.Comparator;
import java.util.List;
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
     *
     * Uniqueness is enforced by a DB unique constraint; in case of race conflicts, the operation retries.
     */
    @Transactional
    public PortReservationEntity createReservation(final AccountEntity account, final tech.amak.portbuddy.server.db.entity.UserEntity user) {
        final var hosts = proxyDiscoveryService.listPublicHosts();
        if (hosts.isEmpty()) {
            throw new IllegalStateException("No available tcp-proxy hosts found");
        }

        final var range = properties.portReservations().range();
        final int min = range.min();
        final int max = range.max();
        if (min <= 0 || max <= 0 || min > max) {
            throw new IllegalStateException("Invalid port range configuration: [" + min + ", " + max + "]");
        }

        int attempts = 0;
        while (attempts++ < MAX_RETRIES) {
            // Order hosts by least reservations
            final var orderedHosts = hosts.stream()
                .sorted(Comparator.comparingLong(repository::countByPublicHost))
                .toList();

            for (final String host : orderedHosts) {
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
                    return saved;
                } catch (final DataIntegrityViolationException e) {
                    // Unique constraint violation possible due to race; retry
                    log.warn("Port reservation conflict for {}:{}, will retry (attempt {}/{})",
                        host, nextPort, attempts, MAX_RETRIES);
                }
            }

            // If we got here, we either had conflicts on all hosts or all were exhausted; retry loop continues
        }

        throw new IllegalStateException("Failed to reserve a unique port after " + MAX_RETRIES + " attempts");
    }

    private Integer computeNextPort(final String host, final int min, final int max) {
        final var maxPortOpt = repository.findMaxPortByHost(host);
        final int next = maxPortOpt.map(p -> p + 1).orElse(min);
        if (next < min) {
            return min;
        }
        if (next > max) {
            return null; // out of range for this host
        }
        return next;
    }

    @Transactional
    public void deleteReservation(final UUID id, final AccountEntity account) {
        final var entity = repository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
        repository.delete(entity);
    }

    /**
     * Resolve a port reservation for a NET (TCP/UDP) expose request according to rules:
     * - If explicit reservation host:port provided, ensure it belongs to the account and is not used by any active tunnel.
     * - Otherwise, if there was a previous tunnel for the same local resource that used a reservation and it's free, reuse it.
     * - Otherwise, pick the first existing reservation of the account that is not in use by any active tunnel.
     * - If none exist, create a new reservation and return it.
     */
    @Transactional
    public PortReservationEntity resolveForNetExpose(final AccountEntity account,
                                                     final tech.amak.portbuddy.server.db.entity.UserEntity user,
                                                     final String localHost,
                                                     final int localPort,
                                                     final String explicitHostPort) {
        // 1) Explicit reservation
        if (explicitHostPort != null && !explicitHostPort.isBlank()) {
            final var hp = explicitHostPort.trim();
            final int colon = hp.lastIndexOf(':');
            if (colon <= 0 || colon == hp.length() - 1) {
                throw new IllegalArgumentException("Invalid --port-reservation value, expected host:port");
            }
            final String host = hp.substring(0, colon);
            final Integer port = Integer.parseInt(hp.substring(colon + 1));
            final var reservation = repository.findByAccountAndHostPort(account, host, port)
                .orElseThrow(() -> new IllegalArgumentException("Port reservation not found for this account: " + hp));
            if (isReservationInUse(reservation)) {
                throw new IllegalStateException("Port reservation is currently in use: " + hp);
            }
            return reservation;
        }

        // 2) Reuse by same local resource if possible
        final var prev = tunnelRepository
            .findFirstByAccountIdAndLocalHostAndLocalPortAndPortReservationIsNotNullOrderByCreatedAtDesc(
                account.getId(), localHost, localPort);
        if (prev.isPresent()) {
            final var res = prev.get().getPortReservation();
            if (res != null && !isReservationInUse(res)) {
                return res;
            }
        }

        // 3) First available among existing reservations
        final var existing = repository.findAllByAccount(account).stream()
            .sorted(Comparator.comparing(PortReservationEntity::getCreatedAt))
            .filter(res -> !isReservationInUse(res))
            .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        // 4) Create new reservation
        return createReservation(account, user);
    }

    private boolean isReservationInUse(final PortReservationEntity reservation) {
        return tunnelRepository.existsByPortReservationAndStatusNot(reservation, TunnelStatus.CLOSED);
    }
}
