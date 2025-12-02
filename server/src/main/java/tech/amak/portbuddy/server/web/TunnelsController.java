/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.entity.TunnelType;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;

@RestController
@RequestMapping(path = "/api/tunnels", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TunnelsController {

    private final TunnelRepository tunnelRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves a paginated list of tunnels associated with the authenticated user's account, ordered by
     * the most recent heartbeat timestamp (nulls last) and creation date.
     *
     * @param principal the authenticated user principle, used to extract the user's unique identifier
     * @param pageable  the pagination and sorting parameters
     * @return a paginated list of {@link TunnelView} objects representing the account's tunnels
     */
    @GetMapping
    public Page<TunnelView> page(final @AuthenticationPrincipal Jwt principal,
                                 final Pageable pageable) {
        final var userId = extractUserId(principal);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var accountId = user.getAccount().getId();
        final Page<TunnelEntity> page = tunnelRepository
            .pageByAccountOrderByLastHeartbeatDescNullsLast(accountId, pageable);
        return page.map(TunnelsController::toView);
    }

    private static TunnelView toView(final TunnelEntity tunnel) {
        final var local = tunnel.getLocalScheme() == null
                          || tunnel.getLocalHost() == null
                          || tunnel.getLocalPort() == null
            ? null
            : "%s://%s:%s".formatted(tunnel.getLocalScheme(), tunnel.getLocalHost(), tunnel.getLocalPort());

        final String publicEndpoint;
        if (tunnel.getType() == TunnelType.HTTP) {
            publicEndpoint = tunnel.getPublicUrl();
        } else {
            final var host = tunnel.getPublicHost();
            final var port = tunnel.getPublicPort();
            publicEndpoint = host == null || port == null ? null : host + ":" + port;
        }

        return new TunnelView(
            tunnel.getId().toString(),
            tunnel.getTunnelId(),
            tunnel.getType(),
            tunnel.getStatus(),
            local,
            publicEndpoint,
            tunnel.getPublicUrl(), // keep original http URL if any
            tunnel.getPublicHost(),
            tunnel.getPublicPort(),
            tunnel.getSubdomain(),
            tunnel.getLastHeartbeatAt() == null ? null : tunnel.getLastHeartbeatAt().toString(),
            tunnel.getCreatedAt() == null ? null : tunnel.getCreatedAt().toString()
        );
    }

    private UUID extractUserId(final Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public record TunnelView(
        String id,
        String tunnelId,
        TunnelType type,
        TunnelStatus status,
        String local,
        String publicEndpoint,
        String publicUrl,
        String publicHost,
        Integer publicPort,
        String subdomain,
        String lastHeartbeatAt,
        String createdAt
    ) {
    }
}
