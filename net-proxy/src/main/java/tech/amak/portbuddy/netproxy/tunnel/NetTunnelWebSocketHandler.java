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

package tech.amak.portbuddy.netproxy.tunnel;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.ControlMessage;
import tech.amak.portbuddy.common.tunnel.MessageEnvelope;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;
import tech.amak.portbuddy.common.utils.IdUtils;
import tech.amak.portbuddy.netproxy.config.AppProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class NetTunnelWebSocketHandler extends AbstractWebSocketHandler {

    private final NetTunnelRegistry registry;
    private final ObjectMapper mapper;
    private final AppProperties properties;

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        final var tunnelId = extractTunnelId(session);
        if (tunnelId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        // Parse query params: type and port
        final var params = parseQueryParams(session.getUri());
        final var typeStr = params.get("type");
        final var portStr = params.get("port");
        if (typeStr == null || portStr == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        final TunnelType tunnelType;
        try {
            tunnelType = "udp".equalsIgnoreCase(typeStr) ? TunnelType.UDP : TunnelType.TCP;
        } catch (final Exception ignore) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        final Integer desiredPort;
        try {
            desiredPort = Integer.parseInt(portStr);
        } catch (final Exception ignore) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // Prepare exposure and then attach the session
        final NetTunnelRegistry.ExposedPort exposedPort;
        try {
            exposedPort = registry.expose(tunnelId, tunnelType, desiredPort);
        } catch (final Exception e) {
            log.warn("Failed to expose {} on {}: {}", tunnelType, desiredPort, e.toString());
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }
        // TODO: validate Authorization header/JWT
        registry.attachSession(tunnelId, session);
        log.info("Net tunnel WS established: {} type={} port={}", tunnelId, tunnelType, desiredPort);

        // Inform client about actual public details in case port was re-assigned
        try {
            final var info = new WsTunnelMessage();
            info.setWsType(WsTunnelMessage.Type.EXPOSED);
            info.setPublicHost(properties.publicHost());
            info.setPublicPort(exposedPort.getPort());
            session.sendMessage(new TextMessage(mapper.writeValueAsString(info)));
        } catch (final Exception e) {
            log.debug("Failed to send EXPOSED info: {}", e.toString());
        }
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage textMessage) throws Exception {
        final var tunnelId = extractTunnelId(session);
        final var payload = textMessage.getPayload();
        // Route by envelope kind: CTRL (heartbeat), WS (control/data)
        final var env = mapper.readValue(payload, MessageEnvelope.class);
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
            return;
        }
        // Unknown kinds are ignored
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
        // Detach session and close exposed sockets for this tunnel immediately
        try {
            final var tunnelId = extractTunnelId(session);
            if (tunnelId != null) {
                registry.closeTunnel(tunnelId);
            }
        } catch (final Exception e) {
            log.debug("Failed to close tunnel on WS close: {}", e.toString());
        } finally {
            registry.detachSession(session);
        }
    }

    private UUID extractTunnelId(final WebSocketSession session) {
        return IdUtils.extractTunnelId(session.getUri());
    }

    private Map<String, String> parseQueryParams(final URI uri) {
        final var map = new HashMap<String, String>();
        if (uri == null) {
            return map;
        }
        final var query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return map;
        }
        final var parts = query.split("&");
        for (final var part : parts) {
            final var idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            final var key = part.substring(0, idx);
            final var value = part.substring(idx + 1);
            map.put(key, value);
        }
        return map;
    }
}
