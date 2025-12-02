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
     * Creates a new HTTP tunnel. This method initializes and saves the HTTP tunnel
     * record in the database with a 'PENDING' status.
     *
     * @param accountId The unique identifier of the account creating the tunnel.
     * @param userId    The unique identifier of the user creating the tunnel.
     * @param apiKeyId  The optional API key identifier associated with the tunnel.
     * @param tunnelId  The unique identifier for the tunnel.
     * @param request   The HTTP expose request containing details of the local HTTP service (scheme, host, port).
     * @param publicUrl The public URL where the service will be accessible.
     * @param domain    The domain entity used for the public HTTP endpoint.
     */
    @Transactional
    public void createHttpTunnel(final UUID accountId,
                                 final UUID userId,
                                 final String apiKeyId,
                                 final String tunnelId,
                                 final HttpExposeRequest request,
                                 final String publicUrl,
                                 final DomainEntity domain) {
        final var entity = new TunnelEntity();
        entity.setId(UUID.randomUUID());
        entity.setTunnelId(tunnelId);
        entity.setType(TunnelType.HTTP);
        entity.setStatus(TunnelStatus.PENDING);
        entity.setAccountId(accountId);
        entity.setUserId(userId);
        if (apiKeyId != null && !apiKeyId.isBlank()) {
            entity.setApiKeyId(UUID.fromString(apiKeyId));
        }
        if (request != null) {
            entity.setLocalScheme(request.scheme());
            entity.setLocalHost(request.host());
            entity.setLocalPort(request.port());
        }
        entity.setPublicUrl(publicUrl);
        entity.setDomain(domain);
        tunnelRepository.save(entity);
        log.info("Created HTTP tunnel record tunnelId={} accountId={} userId={} subdomain={}",
            tunnelId, accountId, userId, domain.getSubdomain());
    }

    /**
     * Creates a new TCP tunnel. This method initializes and saves the TCP tunnel
     * record in the database with a 'PENDING' status.
     *
     * @param accountId  The unique identifier of the account creating the tunnel.
     * @param userId     The unique identifier of the user creating the tunnel.
     * @param apiKeyId   The optional API key identifier associated with the tunnel.
     * @param tunnelId   The unique identifier for the tunnel.
     * @param request    The HTTP expose request containing details of the local service
     *                   (scheme, host, port) to be tunneled.
     * @param publicHost The public host where the service will be exposed.
     * @param publicPort The public port on which the service will be accessible.
     */
    @Transactional
    public void createTcpTunnel(final UUID accountId,
                                final UUID userId,
                                final String apiKeyId,
                                final String tunnelId,
                                final HttpExposeRequest request,
                                final String publicHost,
                                final Integer publicPort) {
        final var entity = new TunnelEntity();
        entity.setId(UUID.randomUUID());
        entity.setTunnelId(tunnelId);
        entity.setType(TunnelType.TCP);
        entity.setStatus(TunnelStatus.PENDING);
        entity.setAccountId(accountId);
        entity.setUserId(userId);
        if (apiKeyId != null && !apiKeyId.isBlank()) {
            entity.setApiKeyId(UUID.fromString(apiKeyId));
        }
        if (request != null) {
            entity.setLocalScheme(request.scheme());
            entity.setLocalHost(request.host());
            entity.setLocalPort(request.port());
        }
        entity.setPublicHost(publicHost);
        entity.setPublicPort(publicPort);
        tunnelRepository.save(entity);
        log.info("Created TCP tunnel record tunnelId={} accountId={} userId={} publicHost={} publicPort={} ",
            tunnelId, accountId, userId, publicHost, publicPort);
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
        tunnelRepository.findByTunnelId(tunnelId).ifPresent(entity -> {
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
        tunnelRepository.findByTunnelId(tunnelId).ifPresent(entity -> {
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
        tunnelRepository.findByTunnelId(tunnelId).ifPresent(entity -> {
            entity.setStatus(TunnelStatus.CLOSED);
            tunnelRepository.save(entity);
        });
    }
}
