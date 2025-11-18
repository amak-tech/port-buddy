package tech.amak.portbuddy.cli.tunnel;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.amak.portbuddy.cli.ui.TcpTrafficSink;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@RequiredArgsConstructor
public class TcpTunnelClient {

    private final String proxyHost;
    private final int proxyHttpPort;
    /**
     * Whether the WebSocket should use TLS (wss). Must reflect the scheme of the configured server URL.
     */
    private final boolean secure;
    private final String tunnelId;
    private final String localHost;
    private final int localPort;
    private final String authToken; // Bearer token if available
    private final TcpTrafficSink trafficSink;

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocket webSocket;

    private final Map<String, LocalTcp> locals = new ConcurrentHashMap<>();
    private final CountDownLatch closed = new CountDownLatch(1);

    /**
     * Establishes and maintains a WebSocket connection for TCP tunneling.
     * This method constructs the WebSocket URL using the configured proxy host, port, and tunnel ID.
     * It sets up an authentication token in the request header, if provided, and initializes the WebSocket connection.
     * The method blocks the current thread until the connection is closed or an interruption occurs.
     * Behavior:
     * - Converts a base HTTP URL to a WebSocket URL using the {@code toWebSocketUrl} method.
     * - Adds an optional "Authorization" header to the WebSocket request for authentication.
     * - Creates a WebSocket connection using the provided URL and the {@code Listener} for handling events.
     * - Waits for the WebSocket connection to close by utilizing a {@code CountDownLatch}.
     * - Handles interruptions by setting the thread's interrupt status.
     */
    public void runBlocking() {
        final var scheme = secure ? "https://" : "http://";
        final var url = toWebSocketUrl(scheme + proxyHost + ":" + proxyHttpPort, "/api/tcp-tunnel/" + tunnelId);
        final var request = new Request.Builder().url(url);
        if (authToken != null && !authToken.isBlank()) {
            request.addHeader("Authorization", "Bearer " + authToken);
        }
        webSocket = http.newWebSocket(request.build(), new Listener());
        try {
            closed.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Closes the WebSocket connection for the TCP tunnel client.
     * This method attempts to gracefully close the WebSocket connection, if it exists,
     * by sending a close frame with a status code of 1000 (normal closure) and a reason
     * message ("Client exit"). If an exception occurs during the close operation, the
     * error is logged for debugging purposes.
     * Behavior:
     * - Checks if the WebSocket instance (`ws`) is not null.
     * - If the WebSocket exists, sends a close frame with a normal status and reason.
     * - Catches and logs any exceptions encountered during the close operation,
     * ensuring the process does not disrupt the program flow.
     */
    public void close() {
        try {
            if (webSocket != null) {
                webSocket.close(1000, "Client exit");
            }
        } catch (final Exception ignore) {
            log.debug("TCP tunnel close error: {}", ignore.toString());
        }
    }

    private String toWebSocketUrl(final String httpUri, final String path) {
        var uri = httpUri;
        if (uri.startsWith("http://")) {
            uri = "ws://" + uri.substring(7);
        } else if (uri.startsWith("https://")) {
            uri = "wss://" + uri.substring(8);
        }
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri + path;
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onOpen(final WebSocket webSocket, final Response response) {
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                final var m = mapper.readValue(text, WsTunnelMessage.class);
                handleControl(m);
            } catch (Exception e) {
                log.warn("Bad control message: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final ByteString bytes) {
            try {
                final var decoded = BinaryWsFrame.decode(bytes.toByteArray());
                if (decoded == null) {
                    return;
                }
                final var local = locals.get(decoded.connectionId());
                if (local != null) {
                    try {
                        local.out.write(decoded.data());
                        local.out.flush();
                        if (trafficSink != null) {
                            trafficSink.onBytesIn(decoded.data().length);
                        }
                    } catch (final Exception e) {
                        log.debug("Write to local TCP failed: {}", e.toString());
                    }
                }
            } catch (final Exception e) {
                log.debug("Failed to handle binary WS frame: {}", e.toString());
            }
        }

        @Override
        public void onClosed(final WebSocket webSocket, final int code, final String reason) {
            log.info("Tunnel closed: {} {}", code, reason);
            closed.countDown();
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable throwable, final Response response) {
            log.warn("Tunnel failure: {}", throwable.toString());
            closed.countDown();
        }
    }

    private void handleControl(final WsTunnelMessage message) throws Exception {
        final var connId = message.getConnectionId();
        switch (message.getWsType()) {
            case OPEN -> {
                // Establish local TCP
                final var socket = new Socket();
                socket.connect(new InetSocketAddress(localHost, localPort), 5000);
                final var local = new LocalTcp(connId, socket);
                locals.put(connId, local);
                // Ack
                final var ack = new WsTunnelMessage();
                ack.setWsType(WsTunnelMessage.Type.OPEN_OK);
                ack.setConnectionId(connId);
                webSocket.send(mapper.writeValueAsString(ack));
                // Start reader thread from local TCP to proxy WS
                new Thread(() -> pumpLocalToProxy(local)).start();
            }
            case BINARY -> {
                // Base64 payload from proxy to local TCP
                final var local = locals.get(connId);
                if (local != null && message.getDataB64() != null) {
                    try {
                        final var bytes = Base64.getDecoder().decode(message.getDataB64());
                        local.out.write(bytes);
                        local.out.flush();
                        if (trafficSink != null) {
                            trafficSink.onBytesIn(bytes.length);
                        }
                    } catch (Exception e) {
                        log.debug("Write to local TCP failed: {}", e.toString());
                    }
                }
            }
            case CLOSE -> {
                final var local = locals.remove(connId);
                if (local != null) {
                    try {
                        local.sock.close();
                    } catch (final Exception ignore) {
                        log.error(ignore.getMessage(), ignore);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void pumpLocalToProxy(final LocalTcp local) {
        final var buffer = new byte[8192];
        try {
            while (true) {
                final var byteCount = local.in.read(buffer);
                if (byteCount == -1) {
                    break;
                }
                final var frame = BinaryWsFrame.encodeToArray(local.connectionId, buffer, 0, byteCount);
                webSocket.send(ByteString.of(frame));
                if (trafficSink != null) {
                    trafficSink.onBytesOut(byteCount);
                }
            }
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                final var message = new WsTunnelMessage();
                message.setWsType(WsTunnelMessage.Type.CLOSE);
                message.setConnectionId(local.connectionId);
                webSocket.send(mapper.writeValueAsString(message));
            } catch (final Exception ignore) {
                log.error("Failed to send local WS close: {}", ignore.toString());
            }
            try {
                local.sock.close();
            } catch (final Exception ignore) {
                log.error("Failed to close local TCP: {}", ignore.toString());
            }
            locals.remove(local.connectionId);
        }
    }

    private static class LocalTcp {
        final String connectionId;
        final Socket sock;
        final InputStream in;
        final OutputStream out;

        LocalTcp(final String connectionId, final Socket sock) throws Exception {
            this.connectionId = connectionId;
            this.sock = sock;
            this.in = sock.getInputStream();
            this.out = sock.getOutputStream();
        }
    }
}
