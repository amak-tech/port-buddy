/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.netproxy.tunnel;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;
import tech.amak.portbuddy.common.utils.IdUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NetTunnelWebSocketHandler extends AbstractWebSocketHandler {

    private final NetTunnelRegistry registry;
    private final ObjectMapper mapper;

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
            case BINARY -> {
                // Backward compatibility: accept base64 text payloads
                registry.onClientBinary(tunnelId, message.getConnectionId(), message.getDataB64());
            }
            case CLOSE -> registry.onClientClose(tunnelId, message.getConnectionId());
            default -> log.debug("Ignoring WS control type: {}", message.getWsType());
        }
    }

    @Override
    protected void handleBinaryMessage(final WebSocketSession session, final BinaryMessage message) {
        final var tunnelId = extractTunnelId(session);
        final var decoded = BinaryWsFrame.decode(message.getPayload());
        if (decoded == null) {
            return;
        }
        registry.onClientBinaryBytes(tunnelId, decoded.connectionId(), decoded.data());
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        // Detach session
        registry.detachSession(session);
    }

    private UUID extractTunnelId(final WebSocketSession session) {
        return IdUtils.extractTunnelId(session.getUri());
    }
}
