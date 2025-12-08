/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.ExposeRequest;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.DomainService;
import tech.amak.portbuddy.server.service.PortReservationService;
import tech.amak.portbuddy.server.service.TunnelService;

@RestController
@RequestMapping(path = "/api/expose", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ExposeController {

    private final AppProperties properties;
    private final TunnelService tunnelService;
    private final UserRepository userRepository;
    private final DomainService domainService;
    private final PortReservationService portReservationService;

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
    @Transactional
    public ExposeResponse exposeHttp(final @AuthenticationPrincipal Jwt jwt,
                                     final @RequestBody ExposeRequest request) {
        final var userId = resolveUserId(jwt);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var account = user.getAccount();

        final var domain = domainService.resolveDomain(
            account, request.domain(), request.host(), request.port());
        final var subdomain = domain.getSubdomain();
        final var gateway = properties.gateway();
        final var publicUrl = "%s://%s.%s".formatted(gateway.schema(), subdomain, gateway.domain());
        final var source = "%s://%s:%s".formatted(request.scheme(), request.host(), request.port());

        final var apiKeyId = extractApiKeyId(jwt);
        final var tunnel = tunnelService.createHttpTunnel(
            account.getId(),
            user.getId(),
            apiKeyId,
            request,
            publicUrl,
            domain);
        final var tunnelId = tunnel.getId();
        return new ExposeResponse(source, publicUrl, null, null, tunnelId, subdomain);
    }

    /**
     * Allocates a public Net port to expose a local TCP or UDP service using the provided request
     * details. This method interacts with a TCP proxy client to assign a unique tunnel ID
     * and configure the TCP exposure.
     *
     * @param request the details of the local TCP or UDP service to expose, including the host,
     *                scheme, and port
     * @return an {@code ExposeResponse} containing the allocated public port, tunnel ID,
     *     and other relevant exposure details
     * @throws RuntimeException if the allocation of the public TCP or UDP port fails
     */
    @PostMapping("/net")
    @Transactional
    public ExposeResponse exposeNet(final @AuthenticationPrincipal Jwt jwt,
                                    final @RequestBody ExposeRequest request) {
        final var userId = resolveUserId(jwt);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var account = user.getAccount();

        final var apiKeyId = extractApiKeyId(jwt);
        // Pre-create tunnel and use its DB id as tunnelId
        final var tunnel = tunnelService.createNetTunnel(account.getId(), user.getId(), apiKeyId, request);
        final var tunnelId = tunnel.getId();

        // Resolve or validate reservation according to rules
        final var reservation = portReservationService.resolveForNetExpose(
            account,
            user,
            request.host(),
            request.port(),
            request.portReservation());

        // Link reservation to tunnel and set public host/port from it
        tunnelService.assignReservation(tunnelId, reservation);

        // Do not call net-proxy here. Return allocated details to CLI.
        return new ExposeResponse(
            "%s %s:%d".formatted(request.tunnelType().name().toLowerCase(), request.host(), request.port()),
            null,
            reservation.getPublicHost(),
            reservation.getPublicPort(),
            tunnelId,
            null);
    }

    private String extractApiKeyId(final Jwt jwt) {
        final var claim = jwt.getClaimAsString("akid");
        return claim == null || claim.isBlank() ? null : claim;
    }
}
