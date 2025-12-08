/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

/**
 * HTTP ingress that forwards requests to a client tunnel by subdomain.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IngressController {

    private final TunnelRegistry registry;
    private final AppProperties properties;

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

    // Path-based fallback ingress for local/dev: http://server/_/{subdomain}/...
    @RequestMapping("/_/{subdomain}/**")
    public void ingressPathBased(final @PathVariable("subdomain") String subdomain,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) throws IOException {
        forwardViaTunnel(subdomain, request, response);
    }

    private void forwardViaTunnel(final String subdomain,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws IOException {
        // If there is no active tunnel for the requested subdomain — redirect users to SPA 404 page
        final var tunnel = registry.getBySubdomain(subdomain);
        if (tunnel == null || !tunnel.isOpen()) {
            final var notFoundUrl = properties.gateway().url() + "/404";
            response.setStatus(307); // Temporary Redirect, preserves method for non-GET
            response.setHeader(HttpHeaders.LOCATION, notFoundUrl);
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
                    for (final var v : values) {
                        if (v != null) {
                            response.addHeader(name, v);
                        }
                    }
                }
            }
            if (resp.getRespBodyB64() != null) {
                final var bytes = Base64.getDecoder().decode(resp.getRespBodyB64());
                response.getOutputStream().write(bytes);
            }
        } catch (final Exception ex) {
            log.warn("Tunnel forward failed for subdomain={}: {}", subdomain, ex.toString());
            response.setStatus(502);
            response.getWriter().write("Bad Gateway: tunnel unavailable");
        }
    }
}
