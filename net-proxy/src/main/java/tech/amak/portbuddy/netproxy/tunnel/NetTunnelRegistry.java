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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class NetTunnelRegistry {

    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 65535;

    private final Map<UUID, Tunnel> byTunnelId = new ConcurrentHashMap<>();
    private final ExecutorService ioPool = Executors.newCachedThreadPool();

    private final ObjectMapper mapper;

    /**
     * Finds and closes a tunnel that is using the specified port for TCP or UDP.
     *
     * @param port the port to check
     */
    private void closeTunnelUsingPort(final int port) {
        for (final var tunnel : byTunnelId.values()) {
            final var tcpPort = tunnel.serverSocket != null ? tunnel.serverSocket.getLocalPort() : -1;
            final var udpPort = tunnel.udpSocket != null ? tunnel.udpSocket.getLocalPort() : -1;

            if (tcpPort == port || udpPort == port) {
                log.info("Closing existing tunnel {} using port {}", tunnel.tunnelId, port);
                closeTunnel(tunnel.tunnelId);
                break;
            }
        }
    }

    /**
     * Exposes a network tunnel for either TCP or UDP based on tunnelType parameter.
     *
     * @param tunnelId   tunnel identifier
     * @param tunnelType tunnelType string ("tcp" or "udp")
     * @return exposed public port info
     * @throws IOException on IO errors
     */
    public ExposedPort expose(final UUID tunnelId, final TunnelType tunnelType, final int desiredPort)
        throws IOException {
        if (desiredPort <= MIN_PORT) {
            throw new IllegalArgumentException("desiredPort must be greater than " + MIN_PORT);
        }
        if (desiredPort > MAX_PORT) {
            throw new IllegalArgumentException("desiredPort must be less than " + MAX_PORT);
        }
        return switch (tunnelType) {
            case UDP -> exposeUdp(tunnelId, desiredPort);
            case TCP -> exposeTcp(tunnelId, desiredPort);
            default -> throw new IllegalArgumentException("Unsupported tunnel type: " + tunnelType);
        };
    }

    /**
     * Expose TCP.
     */
    private ExposedPort exposeTcp(final UUID tunnelId, final Integer desiredPort) throws IOException {
        final var tunnel = byTunnelId.computeIfAbsent(tunnelId, Tunnel::new);
        if (tunnel.serverSocket != null && !tunnel.serverSocket.isClosed()) {
            return new ExposedPort(tunnel.serverSocket.getLocalPort());
        }
        final ServerSocket serverSocket;
        ServerSocket sock;
        try {
            sock = new ServerSocket(desiredPort);
        } catch (final IOException bindEx) {
            log.info("TCP port {} is busy. Trying to close existing tunnel and retry.", desiredPort);
            closeTunnelUsingPort(desiredPort);
            try {
                sock = new ServerSocket(desiredPort);
            } catch (final IOException secondBindEx) {
                log.error("TCP port {} is still busy.", desiredPort);
                throw secondBindEx;
            }
        }
        serverSocket = sock;
        tunnel.serverSocket = serverSocket;
        ioPool.execute(() -> acceptLoop(tunnel));
        return new ExposedPort(serverSocket.getLocalPort());
    }

    /**
     * Expose UDP by binding a datagram socket and starting a receive loop that forwards
     * datagrams over the control WebSocket using binary frames.
     */
    private ExposedPort exposeUdp(final UUID tunnelId, final int desiredPort) throws IOException {

        final var tunnel = byTunnelId.computeIfAbsent(tunnelId, Tunnel::new);
        if (tunnel.udpSocket != null && !tunnel.udpSocket.isClosed()) {
            return new ExposedPort(tunnel.udpSocket.getLocalPort());
        }
        final DatagramSocket socket;
        DatagramSocket sock;
        try {
            sock = new DatagramSocket(desiredPort);
        } catch (final IOException bindEx) {
            log.info("UDP port {} is busy. Trying to close existing tunnel and retry.", desiredPort);
            closeTunnelUsingPort(desiredPort);
            try {
                sock = new DatagramSocket(desiredPort);
            } catch (final IOException secondBindEx) {
                log.error("UDP port {} is still busy.", desiredPort);
                throw secondBindEx;
            }
        }
        socket = sock;
        tunnel.udpSocket = socket;
        ioPool.execute(() -> udpReceiveLoop(tunnel));
        return new ExposedPort(socket.getLocalPort());
    }

    public void attachSession(final UUID tunnelId, final WebSocketSession session) {
        final var tunnel = byTunnelId.computeIfAbsent(tunnelId, Tunnel::new);
        tunnel.session = session;
    }

    /**
     * Detaches a given WebSocket session from any associated tunnel.
     * If the specified session is currently linked to a tunnel, the link is severed.
     *
     * @param session the WebSocket session to detach
     */
    public void detachSession(final WebSocketSession session) {
        for (final var tunnel : byTunnelId.values()) {
            if (tunnel.session == session) {
                tunnel.session = null;
                break;
            }
        }
    }

    /**
     * Closes and removes the entire tunnel identified by the given tunnelId.
     * This will immediately close the TCP ServerSocket (if any), all accepted TCP
     * connections, and the UDP DatagramSocket (if any). Any associated WebSocket
     * session reference is cleared. The tunnel entry is removed from the registry.
     *
     * @param tunnelId identifier of the tunnel to close
     */
    public void closeTunnel(final UUID tunnelId) {
        final var tunnel = byTunnelId.remove(tunnelId);
        if (tunnel == null) {
            return;
        }
        // Close TCP acceptor first so accept loops break
        final var server = tunnel.serverSocket;
        if (server != null) {
            try {
                server.close();
            } catch (final Exception e) {
                log.debug("Failed to close ServerSocket: {}", e.toString());
            }
        }
        // Close all live TCP connections
        for (final var entry : tunnel.connections.entrySet()) {
            final var connection = entry.getValue();
            try {
                connection.socket.close();
            } catch (final Exception e) {
                log.debug("Failed to close connection {}: {}", entry.getKey(), e.toString());
            }
        }
        tunnel.connections.clear();
        // Close UDP socket
        final var udp = tunnel.udpSocket;
        if (udp != null) {
            try {
                udp.close();
            } catch (final Exception e) {
                log.debug("Failed to close DatagramSocket: {}", e.toString());
            }
        }
        tunnel.udpRemotes.clear();
        tunnel.session = null;
    }

    private void acceptLoop(final Tunnel tunnel) {
        try {
            while (!tunnel.serverSocket.isClosed()) {
                final var socket = tunnel.serverSocket.accept();
                final var connId = UUID.randomUUID().toString();
                final var connection = new Connection(connId, socket);
                tunnel.connections.put(connId, connection);
                sendOpen(tunnel, connId);
                // Wait for client OPEN_OK before starting to pump data from public socket
            }
        } catch (final Exception e) {
            log.info("Accept loop ended for tunnel {}: {}", tunnel.tunnelId, e.toString());
        }
    }

    private void pumpFromPublic(final Tunnel tunnel, final Connection connection) {
        final var buffer = new byte[8192];
        try {
            while (true) {
                final var next = connection.in.read(buffer);
                if (next == -1) {
                    break;
                }
                sendBinaryToClient(tunnel, connection.connectionId, buffer, 0, next);
            }
        } catch (final Exception ignore) {
            log.error("Failed to read from public socket: {}", ignore.toString());
        } finally {
            log.info("Public socket closed for tunnel {}: {}", tunnel.tunnelId, connection.connectionId);
            try {
                connection.socket.close();
            } catch (final Exception ignore) {
                log.error("Failed to close public socket: {}", ignore.toString());
            }
            tunnel.connections.remove(connection.connectionId);
            final var message = new WsTunnelMessage();
            message.setWsType(WsTunnelMessage.Type.CLOSE);
            message.setConnectionId(connection.connectionId);
            sendToClient(tunnel, message);
        }
    }

    private void udpReceiveLoop(final Tunnel tunnel) {
        final var buffer = new byte[65535];
        try {
            while (tunnel.udpSocket != null && !tunnel.udpSocket.isClosed()) {
                final var packet = new DatagramPacket(buffer, buffer.length);
                tunnel.udpSocket.receive(packet);
                final var remote = new InetSocketAddress(packet.getAddress(), packet.getPort());
                final var connectionId = remote.getHostString() + ":" + remote.getPort();
                tunnel.udpRemotes.putIfAbsent(connectionId, remote);
                sendBinaryToClient(tunnel, connectionId, packet.getData(), packet.getOffset(), packet.getLength());
            }
        } catch (final Exception e) {
            log.info("UDP receive loop ended for tunnel {}: {}", tunnel.tunnelId, e.toString());
        }
    }

    /**
     * Called when client acknowledges an OPEN with OPEN_OK. Starts pumping data
     * from the public socket to the client over WebSocket for the given connection.
     */
    public void onClientOpenOk(final UUID tunnelId, final String connectionId) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        final var connection = tunnel.connections.get(connectionId);
        if (connection == null) {
            return;
        }
        ioPool.execute(() -> pumpFromPublic(tunnel, connection));
    }

    /**
     * Backward compatibility handler for older clients that still send TEXT frames
     * with base64-encoded payload inside {@link WsTunnelMessage} of type BINARY.
     */
    public void onClientBinary(final UUID tunnelId, final String connectionId, final String dataB64) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        final var connection = tunnel.connections.get(connectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.out.write(Base64.getDecoder().decode(dataB64));
            connection.out.flush();
        } catch (final IOException e) {
            log.debug("Failed to write to public socket: {}", e.toString());
        }
    }

    /**
     * Handles incoming binary WebSocket frames from the client. Data is routed directly
     * to the corresponding public TCP socket without base64 encoding.
     */
    public void onClientBinaryBytes(final UUID tunnelId, final String connectionId, final byte[] data) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        // If UDP is active on this tunnel, route as a datagram
        if (tunnel.udpSocket != null) {
            final var remote = tunnel.udpRemotes.get(connectionId);
            if (remote == null) {
                return;
            }
            try {
                final var packet = new DatagramPacket(data, 0, data.length, remote);
                tunnel.udpSocket.send(packet);
            } catch (final IOException e) {
                log.debug("Failed to send UDP packet: {}", e.toString());
            }
            return;
        }

        // Else assume TCP
        final var connection = tunnel.connections.get(connectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.out.write(data);
            connection.out.flush();
        } catch (final IOException e) {
            log.debug("Failed to write to public socket: {}", e.toString());
        }
    }

    /**
     * Handles the closure of a client connection associated with a specific tunnel.
     * If the tunnel and connection exist, the connection is removed and its socket is closed.
     * If either the tunnel or connection does not exist, no operation is performed.
     *
     * @param tunnelId the identifier of the
     */
    public void onClientClose(final UUID tunnelId, final String connectionId) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        if (tunnel.udpSocket != null) {
            // Just remove mapping; no need to close the UDP socket itself
            tunnel.udpRemotes.remove(connectionId);
        } else {
            final var connection = tunnel.connections.remove(connectionId);
            if (connection != null) {
                try {
                    connection.socket.close();
                } catch (final IOException ignore) {
                    log.error("Failed to close public socket: {}", ignore.toString());
                }
            }
        }
    }

    private void sendOpen(final Tunnel tunnel, final String connId) {
        final var message = new WsTunnelMessage();
        message.setWsType(WsTunnelMessage.Type.OPEN);
        message.setConnectionId(connId);
        sendToClient(tunnel, message);
    }

    private void sendToClient(final Tunnel tunnel, final WsTunnelMessage message) {
        try {
            if (tunnel.session != null && tunnel.session.isOpen()) {
                tunnel.session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            }
        } catch (final IOException e) {
            log.debug("Failed to send to client: {}", e.toString());
        }
    }

    private void sendBinaryToClient(final Tunnel tunnel,
                                    final String connectionId,
                                    final byte[] bytes,
                                    final int offset,
                                    final int length) {
        try {
            if (tunnel.session != null && tunnel.session.isOpen()) {
                final var payload = BinaryWsFrame.encodeToByteBuffer(connectionId, bytes, offset, length);
                tunnel.session.sendMessage(new BinaryMessage(payload));
            }
        } catch (final IOException e) {
            log.debug("Failed to send binary to client: {}", e.toString());
        }
    }

    @Data
    public static class ExposedPort {
        private final int port;
    }

    @Data
    private static class Tunnel {
        private final UUID tunnelId;
        private volatile WebSocketSession session;
        private volatile ServerSocket serverSocket;
        private final Map<String, Connection> connections = new ConcurrentHashMap<>();
        private volatile DatagramSocket udpSocket;
        private final Map<String, InetSocketAddress> udpRemotes = new ConcurrentHashMap<>();

        Tunnel(final UUID tunnelId) {
            this.tunnelId = tunnelId;
        }
    }

    private static class Connection {
        final String connectionId;
        final Socket socket;
        final java.io.InputStream in;
        final java.io.OutputStream out;

        Connection(final String connectionId, final Socket socket) throws IOException {
            this.connectionId = connectionId;
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }
    }
}
