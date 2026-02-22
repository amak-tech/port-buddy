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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.common.TunnelType;

class NetTunnelRegistryTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testBlockHttpOnTcpTunnel() throws IOException, InterruptedException {
        final var registry = new NetTunnelRegistry(mapper);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10001);

        try (final var clientSocket = new Socket("localhost", exposedPort.getPort())) {
            clientSocket.setSoTimeout(5000);

            // Wait for registry to send OPEN
            verify(session, timeout(2000)).sendMessage(any(TextMessage.class));

            // Signal that client is ready (OPEN_OK) to trigger pumpFromPublic
            registry.onClientOpenOk(tunnelId,
                registry.byTunnelId.get(tunnelId).getConnections().keySet().iterator().next());

            final var out = clientSocket.getOutputStream();
            out.write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Wait a bit to see if anything happens
            Thread.sleep(1000);

            // If it blocked, the socket should be closed on the server side
            try {
                final int read = clientSocket.getInputStream().read();
                assertTrue(read == -1, "Socket should be closed by server, but read: " + read);
            } catch (final IOException e) {
                // Connection reset is also a valid indication that the server closed the socket
                assertTrue(e.getMessage().contains("reset") || e.getMessage().contains("closed"),
                    "Expected connection reset or closed, but got: " + e.getMessage());
            }
        } finally {
            registry.closeTunnel(tunnelId);
        }
    }

    @Test
    void testAllowNonHttpOnTcpTunnel() throws IOException, InterruptedException {
        final var registry = new NetTunnelRegistry(mapper);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10002);

        try (final var clientSocket = new Socket("localhost", exposedPort.getPort())) {
            final var out = clientSocket.getOutputStream();
            out.write(new byte[] {0, 1, 2, 3});
            out.flush();

            // Registry should send OPEN message
            verify(session, timeout(1000)).sendMessage(any(TextMessage.class));
        } finally {
            registry.closeTunnel(tunnelId);
        }
    }

    @Test
    void testAllowPostgresSslRequest() throws IOException {
        final var registry = new NetTunnelRegistry(mapper);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10003);

        try (final var clientSocket = new Socket("localhost", exposedPort.getPort())) {
            final var out = clientSocket.getOutputStream();
            // PostgreSQL SSLRequest: Int32(8), Int32(80877103)
            final byte[] sslRequest = {0, 0, 0, 8, 0x04, (byte) 0xd2, 0x16, 0x2f};
            out.write(sslRequest);
            out.flush();

            // Registry should send OPEN message
            verify(session, timeout(1000)).sendMessage(any(TextMessage.class));
        } finally {
            registry.closeTunnel(tunnelId);
        }
    }

    @Test
    void testConnectionWithNoDataSentInitially() throws IOException, InterruptedException {
        final var registry = new NetTunnelRegistry(mapper);
        final var tunnelId = UUID.randomUUID();
        final var session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        registry.attachSession(tunnelId, session);
        final var exposedPort = registry.expose(tunnelId, TunnelType.TCP, 10004);

        try (final var clientSocket = new Socket("localhost", exposedPort.getPort())) {
            // Wait for registry to send OPEN without sending anything from client
            verify(session, timeout(2000)).sendMessage(any(TextMessage.class));
        } finally {
            registry.closeTunnel(tunnelId);
        }
    }
}
