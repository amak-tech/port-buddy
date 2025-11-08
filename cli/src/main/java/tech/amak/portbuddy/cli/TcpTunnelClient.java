package tech.amak.portbuddy.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
public class TcpTunnelClient {

    private final String proxyHost;
    private final int proxyHttpPort;
    private final String tunnelId;
    private final String localHost;
    private final int localPort;
    private final String authToken; // Bearer token if available

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocket ws;

    private final Map<String, LocalTcp> locals = new ConcurrentHashMap<>();
    private final CountDownLatch closed = new CountDownLatch(1);

    public TcpTunnelClient(final String proxyHost, final int proxyHttpPort, final String tunnelId,
                           final String localHost, final int localPort, final String authToken) {
        this.proxyHost = proxyHost;
        this.proxyHttpPort = proxyHttpPort;
        this.tunnelId = tunnelId;
        this.localHost = localHost;
        this.localPort = localPort;
        this.authToken = authToken;
    }

    /**
     * Establishes and maintains a WebSocket connection for TCP tunneling.
     * This method constructs the WebSocket URL using the configured proxy host, port, and tunnel ID.
     * It sets up an authentication token in the request header, if provided, and initializes the WebSocket connection.
     * The method blocks the current thread until the connection is closed or an interruption occurs.
     *
     * <p>
     * Behavior:
     * - Converts a base HTTP URL to a WebSocket URL using the {@code toWebSocketUrl} method.
     * - Adds an optional "Authorization" header to the WebSocket request for authentication.
     * - Creates a WebSocket connection using the provided URL and the {@code Listener} for handling events.
     * - Waits for the WebSocket connection to close by utilizing a {@code CountDownLatch}.
     * - Handles interruptions by setting the thread's interrupt status.
     */
    public void runBlocking() {
        final var url = toWebSocketUrl("http://" + proxyHost + ":" + proxyHttpPort, "/api/tcp-tunnel/" + tunnelId);
        final var rb = new Request.Builder().url(url);
        if (authToken != null && !authToken.isBlank()) {
            rb.addHeader("Authorization", "Bearer " + authToken);
        }
        ws = http.newWebSocket(rb.build(), new Listener());
        try {
            closed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String toWebSocketUrl(final String base, final String path) {
        var u = base;
        if (u.startsWith("http://")) {
            u = "ws://" + u.substring(7);
        } else if (u.startsWith("https://")) {
            u = "wss://" + u.substring(8);
        }
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u + path;
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
            // Not used in this version; receiving data from proxy arrives as TEXT WsTunnelMessage with base64
        }

        @Override
        public void onClosed(final WebSocket webSocket, final int code, final String reason) {
            closed.countDown();
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable t, final Response response) {
            closed.countDown();
        }
    }

    private void handleControl(final WsTunnelMessage m) throws Exception {
        final var connId = m.getConnectionId();
        switch (m.getWsType()) {
            case OPEN -> {
                // Establish local TCP
                final var sock = new Socket();
                sock.connect(new InetSocketAddress(localHost, localPort), 5000);
                final var local = new LocalTcp(connId, sock);
                locals.put(connId, local);
                // Ack
                final var ack = new WsTunnelMessage();
                ack.setWsType(WsTunnelMessage.Type.OPEN_OK);
                ack.setConnectionId(connId);
                ws.send(mapper.writeValueAsString(ack));
                // Start reader thread from local TCP to proxy WS
                new Thread(() -> pumpLocalToProxy(local)).start();
            }
            case BINARY -> {
                // Base64 payload from proxy to local TCP
                final var local = locals.get(connId);
                if (local != null && m.getDataB64() != null) {
                    try {
                        local.out.write(Base64.getDecoder().decode(m.getDataB64()));
                        local.out.flush();
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
        final var buf = new byte[8192];
        try {
            while (true) {
                final var n = local.in.read(buf);
                if (n == -1) {
                    break;
                }
                final var m = new WsTunnelMessage();
                m.setWsType(WsTunnelMessage.Type.BINARY);
                m.setConnectionId(local.connectionId);
                m.setDataB64(Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(buf, n)));
                ws.send(mapper.writeValueAsString(m));
            }
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                final var m = new WsTunnelMessage();
                m.setWsType(WsTunnelMessage.Type.CLOSE);
                m.setConnectionId(local.connectionId);
                ws.send(mapper.writeValueAsString(m));
            } catch (Exception ignore) {
                log.error("Failed to send local WS close: {}", ignore.toString());
            }
            try {
                local.sock.close();
            } catch (Exception ignore) {
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
