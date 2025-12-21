/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.tunnel;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.repo.DomainRepository;

/**
 * Accepts public WebSocket connections from browsers for tunneled subdomains and bridges them
 * over the control WebSocket to the CLI client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicWebSocketProxyHandler extends AbstractWebSocketHandler {

    private final TunnelRegistry registry;
    private final AppProperties properties;
    private final DomainRepository domainRepository;

    @Override
    public void afterConnectionEstablished(final WebSocketSession browserSession) throws Exception {
        var subdomain = extractSubdomain(browserSession);
        if (subdomain == null) {
            log.debug("WS: missing/invalid host header");
            browserSession.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        var tunnel = registry.getBySubdomain(subdomain);
        if (tunnel == null) {
            // It might be a custom domain, try to resolve it to a subdomain
            final var domainOpt = domainRepository.findByCustomDomain(subdomain);
            if (domainOpt.isPresent()) {
                final var resolvedSubdomain = domainOpt.get().getSubdomain();
                tunnel = registry.getBySubdomain(resolvedSubdomain);
            }
        }

        if (tunnel == null || !tunnel.isOpen()) {
            browserSession.close(CloseStatus.SERVICE_RESTARTED);
            return;
        }
        final var connectionId = UUID.randomUUID().toString();
        registry.registerBrowserWs(tunnel.tunnelId(), connectionId, browserSession);

        final var uri = browserSession.getUri();
        final var message = new WsTunnelMessage();
        message.setConnectionId(connectionId);
        message.setWsType(WsTunnelMessage.Type.OPEN);
        if (uri != null) {
            final var normalized = normalizePublicPath(subdomain, uri.getPath());
            message.setPath(normalized);
            message.setQuery(uri.getQuery());
        }

        // Forward essential handshake headers (cookies, origin, vaadin-specific, etc.)
        final var forwarded = collectForwardedHandshakeHeaders(browserSession);
        if (!forwarded.isEmpty()) {
            message.setHeaders(forwarded);
        }

        registry.sendWsToClient(tunnel.tunnelId(), message);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
        final var ids = registry.findIdsByBrowserSession(session);
        if (ids == null) {
            return;
        }
        final var websocketMessage = new WsTunnelMessage();
        websocketMessage.setConnectionId(ids.getConnectionId());
        websocketMessage.setWsType(WsTunnelMessage.Type.TEXT);
        websocketMessage.setText(message.getPayload());
        registry.sendWsToClient(ids.getTunnelId(), websocketMessage);
    }

    @Override
    protected void handleBinaryMessage(final WebSocketSession session, final BinaryMessage message) {
        final var ids = registry.findIdsByBrowserSession(session);
        if (ids == null) {
            return;
        }
        final var websocketMessage = new WsTunnelMessage();
        websocketMessage.setConnectionId(ids.getConnectionId());
        websocketMessage.setWsType(WsTunnelMessage.Type.BINARY);
        websocketMessage.setDataB64(Base64.getEncoder().encodeToString(message.getPayload().array()));
        registry.sendWsToClient(ids.getTunnelId(), websocketMessage);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        final var ids = registry.unregisterBrowserWs(session);
        if (ids == null) {
            return;
        }
        final var websocketMessage = new WsTunnelMessage();
        websocketMessage.setConnectionId(ids.getConnectionId());
        websocketMessage.setWsType(WsTunnelMessage.Type.CLOSE);
        websocketMessage.setCloseCode(status.getCode());
        websocketMessage.setCloseReason(status.getReason());
        registry.sendWsToClient(ids.getTunnelId(), websocketMessage);
    }

    private String extractSubdomain(final WebSocketSession session) {
        // Prefer X-Forwarded-Host because requests are routed via Spring Cloud Gateway,
        // which by default does not preserve the original Host header to the upstream.
        var host = firstNonBlank(
            session.getHandshakeHeaders().getFirst("X-Forwarded-Host"),
            session.getHandshakeHeaders().getFirst(HttpHeaders.HOST)
        );

        if (host != null) {
            // X-Forwarded-Host may contain a comma-separated list — take the first
            final var commaIdx = host.indexOf(',');
            if (commaIdx > 0) {
                host = host.substring(0, commaIdx).trim();
            }
            if (host.endsWith(properties.gateway().subdomainHost())) {
                final var idx = host.indexOf('.');
                if (idx > 0) {
                    return host.substring(0, idx);
                }
            }
        }

        // Fallback: the gateway rewrites path to /_/{subdomain}/... for HTTP
        // and to /_ws/{subdomain}/... for WebSocket handshakes.
        // Try to extract subdomain from the request path if headers are unavailable/unexpected.
        final var uri = session.getUri();
        if (uri != null) {
            final var path = uri.getPath();
            if (path != null) {
                String prefix = null;
                if (path.startsWith("/_ws/")) {
                    prefix = "/_ws/";
                } else if (path.startsWith("/_/")) {
                    prefix = "/_/";
                }
                if (prefix != null) {
                    final var rest = path.substring(prefix.length());
                    final var slash = rest.indexOf('/');
                    if (slash > 0) {
                        return rest.substring(0, slash);
                    }
                    if (!rest.isBlank()) {
                        return rest; // path was exactly /_ws/{subdomain} or /_/{subdomain}
                    }
                }
            }
        }
        return null;
    }

    private static String firstNonBlank(final String a, final String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }

    /**
     * Normalize the path coming from the gateway so the client connects to the local
     * application using its original path. The gateway rewrites incoming public requests
     * to "/_ws/{subdomain}/..." for WebSocket (and "/_/{subdomain}/..." for HTTP).
     * We must strip that internal prefix before forwarding the OPEN to the CLI,
     * otherwise the CLI will try to open a WS to a non-existent path like
     * "/_ws/{subdomain}/..." on the user's local app causing HTTP 200 instead of 101.
     */
    private static String normalizePublicPath(final String subdomain, final String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        var path = rawPath;
        final var wsPrefix = "/_ws/" + subdomain;
        final var httpPrefix = "/_/" + subdomain;
        if (path.startsWith(wsPrefix)) {
            path = path.substring(wsPrefix.length());
        } else if (path.startsWith(httpPrefix)) { // fallback safety
            path = path.substring(httpPrefix.length());
        }
        if (path.isBlank()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    /**
     * Build a safe subset of headers from the browser's WS handshake that should be forwarded
     * to the local application when establishing the tunneled WS connection. This helps
     * frameworks like Vaadin associate the WS with the existing HTTP session (via cookies)
     * and preserve security context.
     */
    private Map<String, String> collectForwardedHandshakeHeaders(final WebSocketSession browserSession) {
        final var result = new HashMap<String, String>();

        final var headers = browserSession.getHandshakeHeaders();

        // Explicit allow-list (case-insensitive)
        final var allowedExact = Set.of(
            "cookie",
            "origin",
            "authorization",
            "referer",
            "x-requested-with",
            "x-csrf-token",
            // Keep subprotocol if requested by the browser (e.g., Vaadin)
            "sec-websocket-protocol"
        );

        // Explicit deny-list of hop-by-hop and WS negotiation headers we must not forward
        final var forbidden = Set.of(
            HttpHeaders.HOST.toLowerCase(),
            HttpHeaders.CONNECTION.toLowerCase(),
            HttpHeaders.UPGRADE.toLowerCase(),
            "sec-websocket-key",
            "sec-websocket-version",
            "sec-websocket-extensions",
            "sec-websocket-accept"
        );

        for (final var name : headers.keySet()) {
            if (name == null) {
                continue;
            }
            final var nlc = name.toLowerCase();
            if (forbidden.contains(nlc)) {
                continue;
            }
            final var isAllowedExact = allowedExact.contains(nlc);
            final var isVaadinSpecific = nlc.startsWith("x-vaadin-") || nlc.startsWith("vaadin-");
            if (isAllowedExact || isVaadinSpecific) {
                final var value = headers.getFirst(name);
                if (value != null && !value.isBlank()) {
                    result.put(name, value);
                }
            }
        }

        // Add/normalize forwarding context headers
        // X-Forwarded-Host: take it from handshake if present, otherwise use Host
        var xfHost = firstNonBlank(headers.getFirst("X-Forwarded-Host"), headers.getFirst(HttpHeaders.HOST));
        if (xfHost != null && !xfHost.isBlank()) {
            final var commaIdx = xfHost.indexOf(',');
            if (commaIdx > 0) {
                xfHost = xfHost.substring(0, commaIdx).trim();
            }
            result.put("X-Forwarded-Host", xfHost);
        }

        // X-Forwarded-Proto: trust existing if present, otherwise derive from ws/wss scheme
        var xfProto = headers.getFirst("X-Forwarded-Proto");
        if (xfProto == null || xfProto.isBlank()) {
            final var uri = browserSession.getUri();
            if (uri != null && uri.getScheme() != null) {
                final var sch = uri.getScheme();
                if ("wss".equalsIgnoreCase(sch)) {
                    xfProto = "https";
                } else if ("ws".equalsIgnoreCase(sch)) {
                    xfProto = "http";
                }
            }
        }
        if (xfProto != null && !xfProto.isBlank()) {
            result.put("X-Forwarded-Proto", xfProto);
        }

        if (!result.isEmpty()) {
            try {
                log.debug("WS: forwarding handshake headers: {}", List.copyOf(result.keySet()));
            } catch (final Exception ignored) {
                // ignore logging issues
            }
        }

        return result;
    }
}
