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
 *
 */

package tech.amak.portbuddy.netproxy.tunnel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.netproxy.config.AppProperties;

class NetTunnelLeakVerificationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AppProperties properties = new AppProperties(
        "localhost",
        new AppProperties.WebSocket(
            DataSize.ofMegabytes(10),
            DataSize.ofMegabytes(10),
            Duration.ofMinutes(10),
            Duration.ofSeconds(10),
            DataSize.ofMegabytes(1)
        ),
        new AppProperties.Jwt("port-buddy", "http://localhost:8080")
    );

    @Test
    void testConnectionCleanupOnSendFailure() throws IOException, InterruptedException {
        final var registry = new NetTunnelRegistry(mapper, properties);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        when(session.isOpen()).thenReturn(true);
        
        // Simulate failure on sendMessage
        doThrow(new IOException("Simulated failure")).when(session).sendMessage(any(BinaryMessage.class));

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10020);

        try (final var clientSocket = new Socket("localhost", exposedPort.getPort())) {
            clientSocket.setSoTimeout(5000);

            // Wait for registry to send OPEN
            verify(session, timeout(2000)).sendMessage(any(TextMessage.class));

            final var tunnel = registry.byTunnelId.get(tunnelId);
            final var connectionId = tunnel.getConnections().keySet().iterator().next();
            
            // Signal OPEN_OK
            registry.onClientOpenOk(tunnelId, connectionId);

            // Send some data from client
            clientSocket.getOutputStream().write("some data".getBytes());
            clientSocket.getOutputStream().flush();

            // The sendBinaryToClient should fail, and pumpFromPublic should exit and cleanup
            // We wait a bit for the async processing
            long start = System.currentTimeMillis();
            while (tunnel.getConnections().containsKey(connectionId) && (System.currentTimeMillis() - start) < 5000) {
                Thread.sleep(100);
            }

            assertNull(tunnel.getConnections().get(connectionId), "Connection should have been cleaned up after send failure");
        } finally {
            registry.closeTunnel(tunnelId);
            registry.shutdown();
        }
    }

    @Test
    void testExecutorShutdown() {
        final var registry = new NetTunnelRegistry(mapper, properties);
        final var ioPool = (ExecutorService) ReflectionTestUtils.getField(registry, "ioPool");
        final var scheduler = (ScheduledExecutorService) ReflectionTestUtils.getField(registry, "scheduler");

        assertFalse(ioPool.isShutdown());
        assertFalse(scheduler.isShutdown());

        registry.shutdown();

        assertTrue(ioPool.isShutdown());
        assertTrue(scheduler.isShutdown());
    }
}
