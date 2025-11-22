package tech.amak.portbuddy.server.tunnel;

import java.util.Base64;
import java.util.Map;
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

    @Override
    public void afterConnectionEstablished(final WebSocketSession browserSession) throws Exception {
        final var subdomain = extractSubdomain(browserSession);
        if (subdomain == null) {
            log.debug("WS: missing/invalid host header");
            browserSession.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        final var tunnel = registry.getBySubdomain(subdomain);
        if (tunnel == null || !tunnel.isOpen()) {
            browserSession.close(CloseStatus.SERVICE_RESTARTED);
            return;
        }
        final var connId = UUID.randomUUID().toString();
        registry.registerBrowserWs(tunnel.tunnelId(), connId, browserSession);

        final var uri = browserSession.getUri();
        final var message = new WsTunnelMessage();
        message.setConnectionId(connId);
        message.setWsType(WsTunnelMessage.Type.OPEN);
        if (uri != null) {
            message.setPath(uri.getPath());
            message.setQuery(uri.getQuery());
        }
        // Forward Sec-WebSocket-Protocol if requested
        final var protocol = browserSession.getHandshakeHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocol != null) {
            message.setHeaders(Map.of("Sec-WebSocket-Protocol", protocol));
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

        // Fallback: the gateway rewrites path to /_/{subdomain}/... for subdomain ingress.
        // Try to extract subdomain from the request path if headers are unavailable/unexpected.
        final var uri = session.getUri();
        if (uri != null) {
            final var path = uri.getPath();
            if (path != null && path.startsWith("/_/")) {
                final var rest = path.substring(3); // after /_/
                final var slash = rest.indexOf('/');
                if (slash > 0) {
                    return rest.substring(0, slash);
                }
                if (!rest.isBlank()) {
                    return rest; // path was exactly /_/{subdomain}
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
}
