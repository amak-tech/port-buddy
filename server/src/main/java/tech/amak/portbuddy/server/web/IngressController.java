/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.springframework.http.HttpStatus.TEMPORARY_REDIRECT;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.DomainRepository;
import tech.amak.portbuddy.server.service.TunnelService;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

/**
 * HTTP ingress that forwards requests to a client tunnel by subdomain.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IngressController {

    private static final String PASSCODE_COOKIE_NAME = "pbp";

    private final TunnelRegistry registry;
    private final AppProperties properties;
    private final DomainRepository domainRepository;
    private final AccountRepository accountRepository;
    private final TunnelService tunnelService;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> HOP_BY_HOP_RESPONSE_HEADERS = Set.of(
        // RFC 7230 hop-by-hop headers + common variants we do not want to relay
        HttpHeaders.CONNECTION.toLowerCase(),
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailer",
        HttpHeaders.TRANSFER_ENCODING.toLowerCase(),
        HttpHeaders.UPGRADE.toLowerCase(),
        // Avoid conflicting length management across hops; let container decide
        HttpHeaders.CONTENT_LENGTH.toLowerCase()
    );

    // HTTP route for subdomain ingress (non-WS traffic)
    @RequestMapping("/_/{subdomain:.+}/**")
    @Transactional
    public void ingressPathBased(final @PathVariable("subdomain") String subdomain,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) throws IOException {
        forwardViaTunnel(subdomain, request, response);
    }

    /**
     * Handles path-based custom domain ingress, mapping requests to the appropriate subdomain.
     * This endpoint processes requests that use the custom domain format in their URL path.
     *
     * @param customDomain The custom domain identifier extracted from the request path. Used to
     *                     locate a matching subdomain in the database.
     * @param request      The incoming HTTP request to be forwarded to the matching subdomain's endpoint.
     * @param response     The HTTP response object used to return output or error codes to the client.
     * @throws IOException If an input or output error occurs during the request forwarding process
     *                     or while setting the HTTP response.
     */
    // Path-based custom domain ingress: http://server/_custom/{customDomain}/...
    @RequestMapping("/_custom/{customDomain:.+}/**")
    @Transactional
    public void ingressCustomDomainPathBased(final @PathVariable("customDomain") String customDomain,
                                             final HttpServletRequest request,
                                             final HttpServletResponse response) throws IOException {
        var lookupDomain = customDomain.toLowerCase();
        final var colonIdx = lookupDomain.indexOf(':');
        if (colonIdx > 0) {
            lookupDomain = lookupDomain.substring(0, colonIdx);
        }

        final var domainOpt = domainRepository.findByCustomDomain(lookupDomain);
        if (domainOpt.isPresent()) {
            forwardViaTunnel(domainOpt.get().getSubdomain(), request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Custom domain not found: " + lookupDomain);
        }
    }

    private void forwardViaTunnel(final String subdomain,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws IOException {
        // If there is no active tunnel for the requested subdomain — redirect users to SPA 404 page
        final var tunnel = registry.getBySubdomain(subdomain);
        if (tunnel == null || !tunnel.isOpen()) {
            final var notFoundUrl = properties.gateway().notFoundPage();
            response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            response.setHeader(HttpHeaders.LOCATION, notFoundUrl);
            return;
        }

        // Check subscription status
        final var accountOpt = accountRepository.findById(tunnel.accountId());
        if (accountOpt.isPresent()) {
            final var status = accountOpt.get().getSubscriptionStatus();
            if (status != null && !"active".equals(status)) {
                log.warn("Blocked request to subdomain {} because subscription is not active (status: {})",
                    subdomain, status);
                response.sendError(HttpServletResponse.SC_PAYMENT_REQUIRED,
                    "Subscription is not active. Please check your billing information.");
                return;
            }
        }

        // Passcode protection check (query param, header, or cookie)
        if (!isAuthorized(subdomain, tunnel.tunnelId(), request, response)) {
            final var gateway = properties.gateway();
            final var originalDomain = "%s.%s".formatted(subdomain, gateway.domain());
            final var redirect = "%s?target_domain=%s".formatted(gateway.passcodePage(), originalDomain);
            response.setStatus(TEMPORARY_REDIRECT.value());
            response.setHeader(HttpHeaders.LOCATION, redirect);
            return;
        }

        final var pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final var bestMatch = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        final var matcher = new AntPathMatcher();
        var path = matcher.extractPathWithinPattern(bestMatch, pathWithin);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        final var method = request.getMethod();
        final var query = request.getQueryString();

        final Map<String, List<String>> headers = new HashMap<>();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
            final var name = en.nextElement();
            // Skip hop-by-hop headers
            if (name.equalsIgnoreCase(HttpHeaders.HOST) || name.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                continue;
            }
            final List<String> values = new ArrayList<>();
            for (Enumeration<String> headerValues = request.getHeaders(name); headerValues.hasMoreElements(); ) {
                final var value = headerValues.nextElement();
                if (value != null) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                headers.put(name, values);
            }
        }

        headers.put("X-Forwarded-Host", List.of(request.getServerName()));
        headers.put("X-Forwarded-Proto", List.of(request.isSecure() ? "https" : "http"));

        final var bodyBytes = request.getInputStream().readAllBytes();
        final var bodyB64 = bodyBytes.length == 0 ? null : Base64.getEncoder().encodeToString(bodyBytes);

        final var msg = new HttpTunnelMessage();
        msg.setMethod(method);
        msg.setPath(path);
        msg.setQuery(query);
        msg.setHeaders(headers);
        msg.setBodyB64(bodyB64);
        msg.setBodyContentType(request.getContentType());

        try {
            final var resp = registry.forwardRequest(subdomain, msg, Duration.ofSeconds(30)).join();
            final var status = resp.getStatus() == null ? 502 : resp.getStatus();
            response.setStatus(status);
            if (resp.getRespHeaders() != null) {
                for (final var header : resp.getRespHeaders().entrySet()) {
                    final var name = header.getKey();
                    final var values = header.getValue();
                    if (name == null || values == null) {
                        continue;
                    }
                    final var nameLc = name.toLowerCase();
                    if (HOP_BY_HOP_RESPONSE_HEADERS.contains(nameLc)) {
                        // Skip hop-by-hop or conflicting headers
                        continue;
                    }
                    values.stream()
                        .filter(Objects::nonNull)
                        .forEach(value ->
                            response.addHeader(name, value));
                }
            }
            if (resp.getRespBodyB64() != null) {
                final var bytes = Base64.getDecoder().decode(resp.getRespBodyB64());
                response.getOutputStream().write(bytes);
            }
        } catch (final Exception ex) {
            log.warn("Tunnel forward failed for subdomain={}: {}", subdomain, ex.toString());
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("Bad Gateway: tunnel unavailable");
        }
    }

    private boolean isAuthorized(final String subdomain,
                                 final UUID tunnelId,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) {

        final var passcodeHash = tunnelService.getTempPasscodeHash(tunnelId)
            .or(() -> domainRepository.findBySubdomain(subdomain)
                .map(DomainEntity::getPasscodeHash))
            .orElse(null);

        // If there is no passcode configured for either the domain or the tunnel — allow access
        if (passcodeHash == null) {
            return true;
        }

        final var passcode = StringUtils.firstNonBlank(
            request.getHeader("X-API-Key"),
            request.getParameter("passcode"));

        // If passcode provided via header or query, validate and set cookie on success
        if (passcode != null) {
            if (matches(passcode, passcodeHash)) {
                issueCookie(response, subdomain, passcode);
                return true;
            }
            return false;
        }

        return findCookie(request, PASSCODE_COOKIE_NAME)
            .map(Cookie::getValue)
            .map(cookiePasscode -> matches(cookiePasscode, passcodeHash))
            .orElse(false);
    }

    private boolean matches(final String raw, final String hash) {
        if (hash == null || raw == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(raw, hash);
        } catch (final Exception e) {
            return false;
        }
    }

    private Optional<Cookie> findCookie(final HttpServletRequest request, final String name) {
        return Stream.ofNullable(request.getCookies())
            .flatMap(Arrays::stream)
            .filter(cookie -> Objects.equals(name, cookie.getName()))
            .findFirst();
    }

    private void issueCookie(final HttpServletResponse response, final String subdomain, final String value) {
        final var gateway = properties.gateway();
        final var cookie = new Cookie(PASSCODE_COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(gateway.url().startsWith("https"));
        cookie.setPath("/");

        // Build a safe cookie domain: strip port and avoid setting Domain for localhost/IP to satisfy RFC6265
        final var configuredDomain = gateway.domain();
        final var domainWithoutPort = configuredDomain.contains(":")
            ? configuredDomain.substring(0, configuredDomain.indexOf(':'))
            : configuredDomain;
        final var fullDomain = subdomain + "." + domainWithoutPort;

        final var isLocalhost = "localhost".equalsIgnoreCase(domainWithoutPort)
                                || domainWithoutPort.endsWith('.' + "localhost");
        final var isIpv4 = domainWithoutPort.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");

        // Only set Domain attribute for real registrable domains (no port, not localhost, not IP)
        final var shouldSetDomain = !(isLocalhost || isIpv4);
        if (shouldSetDomain) {
            cookie.setDomain(fullDomain);
        }

        cookie.setMaxAge(60 * 60 * 12); // 12 hours
        response.addCookie(cookie);

        // Compose manual Set-Cookie with SameSite=Lax; add Domain only when it is valid
        final var sb = new StringBuilder();
        sb.append(PASSCODE_COOKIE_NAME)
            .append("=")
            .append(value)
            .append("; Path=/; Max-Age=43200; HttpOnly; SameSite=Lax");
        if (shouldSetDomain) {
            sb.append("; Domain=").append(fullDomain);
        }
        if (cookie.getSecure()) {
            sb.append("; Secure");
        }
        response.addHeader("Set-Cookie", sb.toString());
    }
}
