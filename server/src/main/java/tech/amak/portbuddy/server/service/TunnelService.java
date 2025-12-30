/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.common.dto.ExposeRequest;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TunnelService {

    public static final List<TunnelStatus> ACTIVE_STATUSES = List.of(TunnelStatus.CONNECTED, TunnelStatus.PENDING);
    private final TunnelRepository tunnelRepository;
    private final AccountRepository accountRepository;

    /**
     * Creates a new HTTP tunnel using the database entity id as the tunnel id.
     * The record is saved with a 'PENDING' status and returned id is used as the tunnel identifier.
     *
     * @param account   The account creating the tunnel.
     * @param userId    The unique identifier of the user creating the tunnel.
     * @param apiKeyId  The optional API key identifier associated with the tunnel.
     * @param request   The HTTP expose request containing details of the local HTTP service (scheme, host, port).
     * @param publicUrl The public URL where the service will be accessible.
     * @param domain    The domain entity used for the public HTTP endpoint.
     * @return the created tunnel id (same as entity id string)
     */
    @Transactional
    public TunnelEntity createHttpTunnel(final AccountEntity account,
                                         final UUID userId,
                                         final String apiKeyId,
                                         final ExposeRequest request,
                                         final String publicUrl,
                                         final DomainEntity domain) {
        checkTunnelLimit(account);
        return createTunnel(account.getId(), userId, apiKeyId, request, publicUrl, domain);
    }

    /**
     * Creates a pending TCP tunnel and returns its tunnel id (entity id).
     * Public host/port can be set later via {@link #updateTunnelPublicConnection(UUID, String, Integer)}.
     *
     * @param account  The account creating the tunnel.
     * @param userId   The unique identifier of the user creating the tunnel.
     * @param apiKeyId The optional API key identifier associated with the tunnel.
     * @param request  The expose request containing details of the local service.
     * @return the created tunnel id (same as entity id string)
     */
    @Transactional
    public TunnelEntity createNetTunnel(final AccountEntity account,
                                        final UUID userId,
                                        final String apiKeyId,
                                        final ExposeRequest request) {
        checkTunnelLimit(account);
        return createTunnel(account.getId(), userId, apiKeyId, request, null, null);
    }

    private void checkSubscriptionStatus(final AccountEntity account) {
        final var status = account.getSubscriptionStatus();
        if (status == null) {
            // Allow Pro plan with 0 extra tunnels without an active subscription record
            if (account.getPlan() == Plan.PRO && account.getExtraTunnels() == 0) {
                return;
            }
            throw new IllegalStateException("No active subscription found. Please check your billing information.");
        }
        if (!"active".equals(status)) {
            throw new IllegalStateException(
                "Subscription is not active (current status: %s). Please check your billing information."
                    .formatted(status));
        }
    }

    private void checkTunnelLimit(final AccountEntity account) {
        checkSubscriptionStatus(account);

        final var currentTunnels = tunnelRepository.countByAccountIdAndStatusIn(account.getId(), ACTIVE_STATUSES);

        final int totalLimit = calculateTunnelLimit(account);

        if (currentTunnels >= totalLimit) {
            throw new IllegalStateException(
                "Tunnel limit reached for your plan (%d). Please upgrade or add more tunnels.".formatted(totalLimit));
        }
    }

    /**
     * Calculates the total tunnel limit for an account (base plan limit + extra tunnels).
     *
     * @param account the account entity
     * @return the total number of allowed tunnels
     */
    public int calculateTunnelLimit(final AccountEntity account) {
        final var plan = account.getPlan();
        final int baseLimit = switch (plan) {
            case PRO -> 1;
            case TEAM -> 10;
        };
        return baseLimit + account.getExtraTunnels();
    }

    /**
     * Checks if the account exceeds its tunnel limit and closes excess tunnels if necessary.
     * Tunnels are closed starting from the ones with no heartbeat or the oldest heartbeat.
     *
     * @param account the account entity to check
     */
    @Transactional
    public void enforceTunnelLimit(final AccountEntity account) {
        final int limit = calculateTunnelLimit(account);
        closeExcessTunnels(account, limit);
    }

    /**
     * Closes all active tunnels for the given account.
     *
     * @param account the account entity
     */
    @Transactional
    public void closeAllTunnels(final AccountEntity account) {
        closeExcessTunnels(account, 0);
    }

    private void closeExcessTunnels(final AccountEntity account, final int limit) {
        final List<TunnelEntity> activeTunnels = tunnelRepository
                .findByAccountIdAndStatusInOrderByLastHeartbeatAtAscCreatedAtAsc(account.getId(), ACTIVE_STATUSES);

        if (activeTunnels.size() > limit) {
            final int toClose = activeTunnels.size() - limit;
            log.info("Account {} has {} active tunnels (limit={}). Closing {} tunnels.",
                account.getId(), activeTunnels.size(), limit, toClose);

            for (int i = 0; i < toClose; i++) {
                final var tunnel = activeTunnels.get(i);
                log.info("Closing tunnel: tunnelId={} accountId={}", tunnel.getId(), account.getId());
                tunnel.setStatus(TunnelStatus.CLOSED);
                tunnelRepository.save(tunnel);
            }
        }
    }

    private TunnelEntity createTunnel(final UUID accountId,
                                      final UUID userId,
                                      final String apiKeyId,
                                      final ExposeRequest request,
                                      final String publicUrl,
                                      final DomainEntity domain) {
        final var tunnel = new TunnelEntity();

        tunnel.setId(UUID.randomUUID());
        tunnel.setType(request.tunnelType());
        tunnel.setStatus(TunnelStatus.PENDING);
        tunnel.setAccountId(accountId);
        tunnel.setUserId(userId);
        tunnel.setLocalScheme(request.scheme());
        tunnel.setLocalHost(request.host());
        tunnel.setLocalPort(request.port());
        tunnel.setPublicUrl(publicUrl);
        tunnel.setDomain(domain);

        if (apiKeyId != null && !apiKeyId.isBlank()) {
            tunnel.setApiKeyId(UUID.fromString(apiKeyId));
        }

        // Ensure non-null timestamps for created_at/updated_at to satisfy DB NOT NULL constraints
        // in case the persistence provider performs an update instead of insert for a new entity.
        tunnel.setCreatedAt(OffsetDateTime.now());
        tunnel.setUpdatedAt(OffsetDateTime.now());

        tunnelRepository.save(tunnel);

        log.info("Created pending {} tunnel record tunnelId={} accountId={} userId={}",
            tunnel.getType(), tunnel.getId(), accountId, userId);

        return tunnel;
    }

    /**
     * Updates public host and port for a TCP tunnel identified by tunnelId.
     *
     * @param tunnelId   The tunnel id (entity id string)
     * @param publicHost Public host allocated by TCP proxy
     * @param publicPort Public port allocated by TCP proxy
     */
    @Transactional
    public void updateTunnelPublicConnection(final UUID tunnelId,
                                             final String publicHost,
                                             final Integer publicPort) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            entity.setPublicHost(publicHost);
            entity.setPublicPort(publicPort);
            tunnelRepository.save(entity);
        });
    }

    /**
     * Updates the tunnel with selected port reservation and sets public host/port from reservation.
     */
    @Transactional
    public void assignReservation(final UUID tunnelId, final PortReservationEntity reservation) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            entity.setPortReservation(reservation);
            entity.setPublicHost(reservation.getPublicHost());
            entity.setPublicPort(reservation.getPublicPort());
            tunnelRepository.save(entity);
        });
    }

    /**
     * Retrieves a tunnel entity based on the provided tunnel ID.
     *
     * @param tunnelId The unique identifier of the tunnel to retrieve.
     * @return An {@code Optional} containing the {@code TunnelEntity} if found,
     *     or an empty {@code Optional} if not found.
     */
    public Optional<TunnelEntity> findByTunnelId(final UUID tunnelId) {
        return Optional.ofNullable(tunnelId)
            .flatMap(tunnelRepository::findById);
    }

    /**
     * Sets a temporary passcode hash on the tunnel entity.
     */
    @Transactional
    public void setTempPasscodeHash(final UUID tunnelId, final String hash) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            entity.setTempPasscodeHash(hash);
            tunnelRepository.save(entity);
        });
    }

    /**
     * Returns the temporary passcode hash for a tunnel, if present.
     */
    public Optional<String> getTempPasscodeHash(final UUID tunnelId) {
        return findByTunnelId(tunnelId).map(TunnelEntity::getTempPasscodeHash);
    }

    /**
     * Updates the status of a tunnel to 'CONNECTED' and sets its last heartbeat
     * timestamp to the current time. This method retrieves the tunnel from the
     * repository using the provided tunnel ID and applies the updates if the
     * tunnel is found.
     *
     * @param tunnelId The unique identifier of the tunnel to update. If null or
     *                 the tunnel is not found, no action is taken.
     */
    @Transactional
    public void markConnected(final UUID tunnelId) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            accountRepository.findById(entity.getAccountId())
                .ifPresent(this::checkSubscriptionStatus);

            entity.setStatus(TunnelStatus.CONNECTED);
            entity.setLastHeartbeatAt(OffsetDateTime.now());
            tunnelRepository.save(entity);
        });
    }

    /**
     * Updates the last heartbeat timestamp of a tunnel to the current time. This method
     * retrieves the tunnel from the repository using the provided tunnel ID and updates
     * the last heartbeat timestamp if the tunnel is found.
     *
     * @param tunnelId The unique identifier of the tunnel whose heartbeat should be updated.
     *                 If null or the tunnel is not found, no action is taken.
     */
    @Transactional
    public void heartbeat(final UUID tunnelId) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            accountRepository.findById(entity.getAccountId())
                .ifPresent(this::checkSubscriptionStatus);

            entity.setLastHeartbeatAt(OffsetDateTime.now());
            tunnelRepository.save(entity);
        });
    }

    /**
     * Updates the status of a tunnel to 'CLOSED'. This method retrieves the tunnel
     * from the repository using the provided tunnel ID and updates the status if
     * the tunnel is found.
     *
     * @param tunnelId The unique identifier of the tunnel to be marked as closed.
     *                 If null or the tunnel is not found, no action is taken.
     */
    @Transactional
    public void markClosed(final UUID tunnelId) {
        findByTunnelId(tunnelId).ifPresent(entity -> {
            entity.setStatus(TunnelStatus.CLOSED);
            tunnelRepository.save(entity);
        });
    }
}
