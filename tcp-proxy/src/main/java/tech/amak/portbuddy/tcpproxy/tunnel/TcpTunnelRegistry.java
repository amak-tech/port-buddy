package tech.amak.portbuddy.tcpproxy.tunnel;

import java.io.IOException;
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
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@Component
public class TcpTunnelRegistry {

    private final Map<String, Tunnel> byTunnelId = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService ioPool = Executors.newCachedThreadPool();

    /**
     * Exposes a TCP tunnel by creating or retrieving a {@link Tunnel} corresponding to the provided
     * tunnel ID and initializes a {@link ServerSocket} for incoming connections. If the tunnel already
     * exists and its server socket is open, it returns the existing exposed port. Otherwise, a new
     * server socket is created, bound to an available port, and an accept loop is started to handle
     * incoming connections. The exposed local port is returned in an {@link ExposedPort} object.
     *
     * @param tunnelId the identifier of the tunnel to be exposed; if no tunnel exists for this ID,
     *                 a new one is created.
     * @return the {@link ExposedPort} object containing the TCP port exposed by the tunnel.
     * @throws IOException if an I/O error occurs during the initialization of the server socket.
     */
    public ExposedPort expose(final String tunnelId) throws IOException {
        final var tunnel = byTunnelId.computeIfAbsent(tunnelId, Tunnel::new);
        if (tunnel.serverSocket != null && !tunnel.serverSocket.isClosed()) {
            return new ExposedPort(tunnel.serverSocket.getLocalPort());
        }
        final var serverSocket = new ServerSocket(0);
        tunnel.serverSocket = serverSocket;
        ioPool.execute(() -> acceptLoop(tunnel));
        return new ExposedPort(serverSocket.getLocalPort());
    }

    public void attachSession(final String tunnelId, final WebSocketSession session) {
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

    /**
     * Called when client acknowledges an OPEN with OPEN_OK. Starts pumping data
     * from the public socket to the client over WebSocket for the given connection.
     */
    public void onClientOpenOk(final String tunnelId, final String connectionId) {
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
    public void onClientBinary(final String tunnelId, final String connectionId, final String dataB64) {
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
        } catch (IOException e) {
            log.debug("Failed to write to public socket: {}", e.toString());
        }
    }

    /**
     * Handles incoming binary WebSocket frames from the client. Data is routed directly
     * to the corresponding public TCP socket without base64 encoding.
     */
    public void onClientBinaryBytes(final String tunnelId, final String connectionId, final byte[] data) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        final var connection = tunnel.connections.get(connectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.out.write(data);
            connection.out.flush();
        } catch (IOException e) {
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
    public void onClientClose(final String tunnelId, final String connectionId) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        final var connection = tunnel.connections.remove(connectionId);
        if (connection != null) {
            try {
                connection.socket.close();
            } catch (IOException ignore) {
                log.error("Failed to close public socket: {}", ignore.toString());
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
        } catch (IOException e) {
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
        } catch (IOException e) {
            log.debug("Failed to send binary to client: {}", e.toString());
        }
    }

    @Data
    public static class ExposedPort {
        private final int port;
    }

    @Data
    private static class Tunnel {
        private final String tunnelId;
        private volatile WebSocketSession session;
        private volatile ServerSocket serverSocket;
        private final Map<String, Connection> connections = new ConcurrentHashMap<>();

        Tunnel(final String tunnelId) {
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
