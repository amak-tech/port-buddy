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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.exception.SubscriptionException;
import tech.amak.portbuddy.server.service.TunnelService;

@ExtendWith(MockitoExtension.class)
class TunnelWebSocketHandlerTest {

    @Mock
    private TunnelRegistry registry;
    @Mock
    private TunnelService tunnelService;
    @Mock
    private WebSocketSession session;

    private TunnelWebSocketHandler handler;
    private UUID tunnelId;
    private TunnelEntity tunnel;

    @BeforeEach
    void setUp() {
        handler = new TunnelWebSocketHandler(registry, new ObjectMapper(), tunnelService);
        tunnelId = UUID.randomUUID();
        tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        when(session.getUri()).thenReturn(URI.create("wss://portbuddy.dev/api/http-tunnel/" + tunnelId));
    }

    @Test
    void afterConnectionEstablished_SubscriptionInvalid_RejectsWithoutRegistering() throws Exception {
        when(tunnelService.findByTunnelId(tunnelId)).thenReturn(Optional.of(tunnel));
        doThrow(new SubscriptionException("Subscription is not active")).when(tunnelService).markConnected(tunnelId);
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(registry, never()).register(any(TunnelEntity.class), any());
        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void afterConnectionEstablished_SubscriptionValid_Registers() {
        when(tunnelService.findByTunnelId(tunnelId)).thenReturn(Optional.of(tunnel));

        handler.afterConnectionEstablished(session);

        verify(tunnelService).markConnected(tunnelId);
        verify(registry).register(tunnel, session);
    }
}
