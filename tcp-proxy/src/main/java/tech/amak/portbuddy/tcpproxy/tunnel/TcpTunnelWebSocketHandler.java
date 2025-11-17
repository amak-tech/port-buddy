package tech.amak.portbuddy.tcpproxy.tunnel;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpTunnelWebSocketHandler extends TextWebSocketHandler {

    private final TcpTunnelRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        final var tunnelId = extractTunnelId(session);
        if (tunnelId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        // TODO: validate Authorization header/JWT
        registry.attachSession(tunnelId, session);
        log.info("TCP tunnel WS established: {}", tunnelId);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage textMessage) throws Exception {
        final var tunnelId = extractTunnelId(session);
        final var payload = textMessage.getPayload();
        final var message = mapper.readValue(payload, WsTunnelMessage.class);
        switch (message.getWsType()) {
            case OPEN_OK -> registry.onClientOpenOk(tunnelId, message.getConnectionId());
            case BINARY -> registry.onClientBinary(tunnelId, message.getConnectionId(), message.getDataB64());
            case CLOSE -> registry.onClientClose(tunnelId, message.getConnectionId());
            default -> log.debug("Ignoring WS control type: {}", message.getWsType());
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) throws Exception {
        // Detach session
        registry.detachSession(session);
    }

    private String extractTunnelId(final WebSocketSession session) {
        final var uri = session.getUri();
        if (uri == null) {
            return null;
        }
        final var parts = uri.getPath().split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
