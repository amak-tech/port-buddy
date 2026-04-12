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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.netproxy.config.AppProperties;

@Slf4j
class NetTunnelRegistryConcurrencyTest {

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
    void testManyConcurrentConnections() throws IOException, InterruptedException {
        final var registry = new NetTunnelRegistry(mapper, properties);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10005);

        final int connectionCount = 300; // More than the previous 200 limit
        final List<Socket> sockets = new ArrayList<>();

        try {
            for (int i = 0; i < connectionCount; i++) {
                sockets.add(new Socket("localhost", exposedPort.getPort()));
            }

            // Give it a moment to process all connections
            Thread.sleep(2000);

            // Check that all connections are registered
            final var tunnel = registry.byTunnelId.get(tunnelId);
            assertEquals(connectionCount, tunnel.getConnections().size(),
                "Should have " + connectionCount + " active connections");

        } finally {
            for (final var socket : sockets) {
                try {
                    socket.close();
                } catch (final IOException ignore) {
                    log.debug("Failed to close socket during test cleanup", ignore);
                }
            }
            registry.closeTunnel(tunnelId);
        }
    }
}
