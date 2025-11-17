package tech.amak.portbuddy.server.tunnel;

import java.net.URI;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelWebSocketHandler extends TextWebSocketHandler {

    private final TunnelRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        final var uri = session.getUri();
        final var tunnelId = extractTunnelId(uri);
        if (tunnelId == null || !registry.attachSession(tunnelId, session)) {
            log.warn("Tunnel not found for id={}", tunnelId);
            session.close(CloseStatus.NORMAL);
            return;
        }
        log.info("Tunnel session established: {}", tunnelId);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        try {
            log.debug("Received message from client: {}", message.getPayload());
            final var uri = session.getUri();
            final var tunnelId = extractTunnelId(uri);
            final String payload = message.getPayload();
            final JsonNode node = mapper.readTree(payload);
            if (node.has("kind") && "WS".equals(node.get("kind").asText())) {
                final var wsMsg = mapper.treeToValue(node, WsTunnelMessage.class);
                handleWsFromClient(tunnelId, wsMsg);
                return;
            }
            final var httpMsg = mapper.treeToValue(node, HttpTunnelMessage.class);
            if (httpMsg.getType() == HttpTunnelMessage.Type.RESPONSE) {
                registry.onResponse(tunnelId, httpMsg);
            } else {
                log.debug("Ignoring unexpected message type from client: {}", httpMsg.getType());
            }
        } catch (final Exception e) {
            log.warn("Tunnel message handling error: {}", e.toString());
        }
    }

    private void handleWsFromClient(final String tunnelId, final WsTunnelMessage message) throws Exception {
        final var browser = registry.getBrowserSession(tunnelId, message.getConnectionId());
        if (browser == null) {
            log.debug("No browser WS for connectionId={} tunnelId={}", message.getConnectionId(), tunnelId);
            return;
        }
        switch (message.getWsType()) {
            case OPEN_OK -> { /* nothing extra for now */ }
            case TEXT -> browser.sendMessage(new TextMessage(message.getText() != null ? message.getText() : ""));
            case BINARY -> {
                if (message.getDataB64() != null) {
                    final var bytes = Base64.getDecoder().decode(message.getDataB64());
                    browser.sendMessage(new org.springframework.web.socket.BinaryMessage(bytes));
                }
            }
            case CLOSE -> {
                final var code = message.getCloseCode() != null ? message.getCloseCode() : CloseStatus.NORMAL.getCode();
                final var reason = message.getCloseReason();
                browser.close(new CloseStatus(code, reason));
            }
            default -> {
            }
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) throws Exception {
        final var uri = session.getUri();
        final var tunnelId = extractTunnelId(uri);
        final var tunnel = registry.getByTunnelId(tunnelId);
        if (tunnel != null) {
            tunnel.setSession(null);
            log.info("Tunnel session closed: {}", tunnelId);
        }
    }

    private String extractTunnelId(final URI uri) {
        if (uri == null) {
            return null;
        }
        final var path = uri.getPath();
        if (path == null) {
            return null;
        }
        final var parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
