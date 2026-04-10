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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;

class TunnelRegistryLeakTest {

    @Test
    void shouldCloseBrowserSessionsWhenTunnelIsClosed() throws Exception {
        // Given
        final var mapper = new ObjectMapper();
        final var registry = new TunnelRegistry(mapper);

        final var tunnelId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setSubdomain("test");
        final var tunnelEntity = new TunnelEntity();
        tunnelEntity.setId(tunnelId);
        tunnelEntity.setAccountId(accountId);
        tunnelEntity.setDomain(domain);

        final var tunnelSession = mock(WebSocketSession.class);
        when(tunnelSession.isOpen()).thenReturn(true);
        registry.register(tunnelEntity, tunnelSession);

        final var browserSession = mock(WebSocketSession.class);
        when(browserSession.isOpen()).thenReturn(true);
        final var connectionId = "conn-1";
        registry.registerBrowserWs(tunnelId, connectionId, browserSession);

        // Verify it is registered
        assert registry.getByTunnelId(tunnelId) != null;
        assert registry.getBySubdomain("test") != null;

        // When
        registry.closeTunnel(tunnelId);

        // Then - the browser session should be closed
        verify(browserSession).close();

        // And it should be removed from maps
        assert registry.getByTunnelId(tunnelId) == null;
        assert registry.getBySubdomain("test") == null;
    }

    @Test
    void shouldCleanupPendingRequestsOnTimeout() throws Exception {
        // Given
        final var mapper = new ObjectMapper();
        final var registry = new TunnelRegistry(mapper);

        final var tunnelId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setSubdomain("test");
        final var tunnelEntity = new TunnelEntity();
        tunnelEntity.setId(tunnelId);
        tunnelEntity.setAccountId(accountId);
        tunnelEntity.setDomain(domain);

        final var tunnelSession = mock(WebSocketSession.class);
        when(tunnelSession.isOpen()).thenReturn(true);
        registry.register(tunnelEntity, tunnelSession);

        final var request = new tech.amak.portbuddy.common.tunnel.HttpTunnelMessage();
        request.setId("req-1");

        // When
        final var future = registry.forwardRequest("test", request, java.time.Duration.ofMillis(100));

        final var tunnel = registry.getByTunnelId(tunnelId);
        assert tunnel.pending().containsKey("req-1");

        // Wait for timeout
        Thread.sleep(200);

        // Then
        assert future.isCompletedExceptionally();
        assert !tunnel.pending().containsKey("req-1");
    }

    @Test
    void shouldCleanupBrowserReverseMapWhenUnregistered() throws Exception {
        // Given
        final var mapper = new ObjectMapper();
        final var registry = new TunnelRegistry(mapper);

        final var tunnelId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setSubdomain("test");
        final var tunnelEntity = new TunnelEntity();
        tunnelEntity.setId(tunnelId);
        tunnelEntity.setAccountId(accountId);
        tunnelEntity.setDomain(domain);

        final var tunnelSession = mock(WebSocketSession.class);
        when(tunnelSession.isOpen()).thenReturn(true);
        registry.register(tunnelEntity, tunnelSession);

        final var browserSession = mock(WebSocketSession.class);
        final var connectionId = "conn-1";
        registry.registerBrowserWs(tunnelId, connectionId, browserSession);

        final var tunnel = registry.getByTunnelId(tunnelId);
        assert tunnel.browserByConnection().containsKey(connectionId);
        assert tunnel.browserReverse().containsKey(browserSession);

        // When
        registry.unregisterBrowserWs(browserSession);

        // Then
        assert !tunnel.browserByConnection().containsKey(connectionId);
        assert !tunnel.browserReverse().containsKey(browserSession);
    }
}
