/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;
import tech.amak.portbuddy.server.client.TcpProxyClient;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.DomainService;
import tech.amak.portbuddy.server.service.TunnelService;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

@RestController
@RequestMapping(path = "/api/expose", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ExposeController {

    private final TunnelRegistry registry;
    private final AppProperties properties;
    private final TcpProxyClient tcpProxyClient;
    private final TunnelService tunnelService;
    private final UserRepository userRepository;
    private final DomainService domainService;

    /**
     * Creates a public HTTP endpoint to expose a local HTTP service by generating a unique
     * subdomain and tunnel ID. The method constructs a public URL based on the application
     * gateway settings and links it to the provided local service details (scheme, host, and port).
     *
     * @param request the details of the local HTTP service to expose, including the scheme,
     *                host, and port
     * @return an {@code ExposeResponse} containing the source (local service details),
     *     the generated public URL, tunnel ID, and subdomain information for the exposed service
     */
    @PostMapping("/http")
    public ExposeResponse exposeHttp(final @AuthenticationPrincipal Jwt jwt,
                                     final @RequestBody HttpExposeRequest request) {
        final var userId = resolveUserId(jwt);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var account = user.getAccount();

        final var domainEntity = domainService.resolveDomain(
            account, userId, request.domain(), request.host(), request.port());
        final var subdomain = domainEntity.getSubdomain();

        final var tunnelId = UUID.randomUUID().toString();
        registry.createPending(subdomain, tunnelId);
        final var gateway = properties.gateway();
        final var publicUrl = "%s://%s.%s".formatted(gateway.schema(), subdomain, gateway.domain());
        final var source = "%s://%s:%s".formatted(request.scheme(), request.host(), request.port());

        final var apiKeyId = extractApiKeyId(jwt);
        tunnelService.createHttpTunnel(userId, apiKeyId, tunnelId, request, publicUrl, subdomain);
        return new ExposeResponse(source, publicUrl, null, null, tunnelId, subdomain);
    }

    /**
     * Allocates a public TCP port to expose a local TCP service using the provided request
     * details. This method interacts with a TCP proxy client to assign a unique tunnel ID
     * and configure the TCP exposure.
     *
     * @param request the details of the local TCP service to expose, including the host,
     *                scheme, and port
     * @return an {@code ExposeResponse} containing the allocated public port, tunnel ID,
     *     and other relevant exposure details
     * @throws RuntimeException if the allocation of the public TCP port fails
     */
    @PostMapping("/tcp")
    public ExposeResponse exposeTcp(final @AuthenticationPrincipal Jwt jwt,
                                    final @RequestBody HttpExposeRequest request) {
        final var tunnelId = UUID.randomUUID().toString();

        // Ask the selected tcp-proxy to allocate a public TCP port for this tunnelId
        try {
            final var exposeResponse = tcpProxyClient.exposePort(tunnelId);
            log.info("Expose TCP port response: {}", exposeResponse);
            final var userId = resolveUserId(jwt);
            final var apiKeyId = extractApiKeyId(jwt);
            tunnelService.createTcpTunnel(userId, apiKeyId, tunnelId, request,
                exposeResponse.publicHost(), exposeResponse.publicPort());
            return exposeResponse;
        } catch (final Exception e) {
            log.error("Failed to allocate public TCP port for tunnelId={}: {}", tunnelId, e.getMessage(), e);
        }

        throw new RuntimeException("Failed to allocate public TCP port for tunnelId=" + tunnelId);
    }

    private String extractApiKeyId(final Jwt jwt) {
        final var claim = jwt.getClaimAsString("akid");
        return claim == null || claim.isBlank() ? null : claim;
    }
}
