/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.entity.TunnelType;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TunnelService {

    private final TunnelRepository tunnelRepository;

    /**
     * Creates a new HTTP tunnel using the database entity id as the tunnel id.
     * The record is saved with a 'PENDING' status and returned id is used as the tunnel identifier.
     *
     * @param accountId The unique identifier of the account creating the tunnel.
     * @param userId    The unique identifier of the user creating the tunnel.
     * @param apiKeyId  The optional API key identifier associated with the tunnel.
     * @param request   The HTTP expose request containing details of the local HTTP service (scheme, host, port).
     * @param publicUrl The public URL where the service will be accessible.
     * @param domain    The domain entity used for the public HTTP endpoint.
     * @return the created tunnel id (same as entity id string)
     */
    @Transactional
    public TunnelEntity createHttpTunnel(final UUID accountId,
                                         final UUID userId,
                                         final String apiKeyId,
                                         final HttpExposeRequest request,
                                         final String publicUrl,
                                         final DomainEntity domain) {
        final var tunnel = new TunnelEntity();
        final var id = UUID.randomUUID();
        tunnel.setId(id);
        tunnel.setType(TunnelType.HTTP);
        tunnel.setStatus(TunnelStatus.PENDING);
        tunnel.setAccountId(accountId);
        tunnel.setUserId(userId);
        if (apiKeyId != null && !apiKeyId.isBlank()) {
            tunnel.setApiKeyId(UUID.fromString(apiKeyId));
        }
        if (request != null) {
            tunnel.setLocalScheme(request.scheme());
            tunnel.setLocalHost(request.host());
            tunnel.setLocalPort(request.port());
        }
        tunnel.setPublicUrl(publicUrl);
        tunnel.setDomain(domain);
        tunnelRepository.save(tunnel);
        log.info("Created HTTP tunnel record tunnelId={} accountId={} userId={} subdomain={}",
            id, accountId, userId, domain.getSubdomain());
        return tunnel;
    }

    /**
     * Creates a pending TCP tunnel and returns its tunnel id (entity id).
     * Public host/port can be set later via {@link #updateTcpTunnelPublic(String, String, Integer)}.
     *
     * @param accountId The unique identifier of the account creating the tunnel.
     * @param userId    The unique identifier of the user creating the tunnel.
     * @param apiKeyId  The optional API key identifier associated with the tunnel.
     * @param request   The expose request containing details of the local service.
     * @return the created tunnel id (same as entity id string)
     */
    @Transactional
    public TunnelEntity createPendingTcpTunnel(final UUID accountId,
                                               final UUID userId,
                                               final String apiKeyId,
                                               final HttpExposeRequest request) {
        final var tunnel = new TunnelEntity();
        final var id = UUID.randomUUID();
        tunnel.setId(id);
        tunnel.setType(TunnelType.TCP);
        tunnel.setStatus(TunnelStatus.PENDING);
        tunnel.setAccountId(accountId);
        tunnel.setUserId(userId);
        if (apiKeyId != null && !apiKeyId.isBlank()) {
            tunnel.setApiKeyId(UUID.fromString(apiKeyId));
        }
        if (request != null) {
            tunnel.setLocalScheme(request.scheme());
            tunnel.setLocalHost(request.host());
            tunnel.setLocalPort(request.port());
        }
        tunnelRepository.save(tunnel);
        log.info("Created pending TCP tunnel record tunnelId={} accountId={} userId={}",
            id, accountId, userId);
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
    public void updateTcpTunnelPublic(final UUID tunnelId,
                                      final String publicHost,
                                      final Integer publicPort) {
        if (tunnelId == null) {
            return;
        }
        tunnelRepository.findById(tunnelId).ifPresent(entity -> {
            entity.setPublicHost(publicHost);
            entity.setPublicPort(publicPort);
            tunnelRepository.save(entity);
        });
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
    public void markConnected(final String tunnelId) {
        if (tunnelId == null) {
            return;
        }
        final var id = parseUuid(tunnelId);
        if (id == null) {
            return;
        }
        tunnelRepository.findById(id).ifPresent(entity -> {
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
    public void heartbeat(final String tunnelId) {
        if (tunnelId == null) {
            return;
        }
        final var id = parseUuid(tunnelId);
        if (id == null) {
            return;
        }
        tunnelRepository.findById(id).ifPresent(entity -> {
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
    public void markClosed(final String tunnelId) {
        if (tunnelId == null) {
            return;
        }
        final var id = parseUuid(tunnelId);
        if (id == null) {
            return;
        }
        tunnelRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(TunnelStatus.CLOSED);
            tunnelRepository.save(entity);
        });
    }

    private UUID parseUuid(final String id) {
        try {
            return UUID.fromString(id);
        } catch (final Exception e) {
            log.warn("Invalid tunnelId format: {}", id);
            return null;
        }
    }
}
