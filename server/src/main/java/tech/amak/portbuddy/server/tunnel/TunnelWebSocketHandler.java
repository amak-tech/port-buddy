/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.server.tunnel;

import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.ControlMessage;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.MessageEnvelope;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;
import tech.amak.portbuddy.common.utils.IdUtils;
import tech.amak.portbuddy.server.service.TunnelService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelWebSocketHandler extends TextWebSocketHandler {

    private final TunnelRegistry registry;
    private final ObjectMapper mapper;
    private final TunnelService tunnelService;

    @Override
    @Transactional
    public void afterConnectionEstablished(final WebSocketSession session) {
        final var tunnelId = extractTunnelId(session);

        tunnelService.findByTunnelId(tunnelId).ifPresentOrElse(
            tunnel -> {
                registry.register(tunnel, session);
                tunnelService.markConnected(tunnelId);
                log.info("Tunnel session established: {}", tunnelId);
            },
            () -> {
                log.warn("Tunnel not found for id={}", tunnelId);
                closeWebsocket(session, CloseStatus.NORMAL);
            });
    }

    private void closeWebsocket(final WebSocketSession session,
                                final CloseStatus status) {
        try {
            session.close(status);
            log.debug("Closed websocket. Status: {}, URI: {}", status, session.getUri());
        } catch (final Exception e) {
            log.warn("Failed to close websocket: {}", e.toString());
        }
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
        try {
            log.trace("Received message from client: {}", message.getPayload());
            final var tunnelId = extractTunnelId(session);

            tunnelService.heartbeat(tunnelId);

            final String payload = message.getPayload();
            final var env = mapper.readValue(payload, MessageEnvelope.class);
            // Control health checks
            if (env.getKind() != null && env.getKind().equals("CTRL")) {
                final var ctrl = mapper.readValue(payload, ControlMessage.class);
                if (ctrl.getType() == ControlMessage.Type.PING) {
                    final var pong = new ControlMessage();
                    pong.setType(ControlMessage.Type.PONG);
                    pong.setTs(System.currentTimeMillis());
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(pong)));
                }
                return;
            }
            if (env.getKind() != null && env.getKind().equals("WS")) {
                final var wsMsg = mapper.readValue(payload, WsTunnelMessage.class);
                handleWsFromClient(tunnelId, wsMsg);
                return;
            }
            final var httpMsg = mapper.readValue(payload, HttpTunnelMessage.class);
            if (httpMsg.getType() == HttpTunnelMessage.Type.RESPONSE) {
                registry.onResponse(tunnelId, httpMsg);
            } else {
                log.debug("Ignoring unexpected message type from client: {}", httpMsg.getType());
            }
        } catch (final Exception e) {
            log.warn("Tunnel message handling error: {}", e.toString());
        }
    }

    private void handleWsFromClient(final UUID tunnelId, final WsTunnelMessage message) throws Exception {
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
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        final var tunnelId = extractTunnelId(session);
        final var tunnel = registry.getByTunnelId(tunnelId);
        if (tunnel != null) {
            tunnel.setSession(null);
            log.info("Tunnel session closed: {} code={} reason={}", tunnelId,
                status != null ? status.getCode() : null,
                status != null ? status.getReason() : null);
        }
        tunnelService.markClosed(tunnelId);
    }

    private UUID extractTunnelId(final WebSocketSession session) {
        return IdUtils.extractTunnelId(session.getUri());
    }
}
