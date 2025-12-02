/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.cli.tunnel;

import static tech.amak.portbuddy.cli.utils.JsonUtils.MAPPER;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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
    private final OkHttpClient rest = new OkHttpClient();
    private WebSocket webSocket;

    private final Map<String, LocalTcp> locals = new ConcurrentHashMap<>();
    private CountDownLatch closed = new CountDownLatch(1);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "port-buddy-tcp-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private volatile ScheduledFuture<?> heartbeatTask;
    private final AtomicBoolean closedReported = new AtomicBoolean(false);
    private final AtomicBoolean stop = new AtomicBoolean(false);

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
        var backoffMs = 1000L;
        final var maxBackoffMs = 30000L;
        while (!stop.get()) {
            try {
                closed = new CountDownLatch(1);
                final var scheme = secure ? "https://" : "http://";
                final var url = toWebSocketUrl(scheme + proxyHost + ":" + proxyHttpPort, "/api/tcp-tunnel/" + tunnelId);
                final var request = new Request.Builder().url(url);
                if (authToken != null && !authToken.isBlank()) {
                    request.addHeader("Authorization", "Bearer " + authToken);
                }
                webSocket = http.newWebSocket(request.build(), new Listener());

                // Block until this connection is closed
                closed.await();
                if (stop.get()) {
                    break;
                }
                // Reconnect with backoff
                log.info("TCP tunnel disconnected; reconnecting in {} ms...", backoffMs);
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                log.warn("TCP tunnel loop error: {}", e.toString());
                try {
                    Thread.sleep(backoffMs);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            }
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
            stop.set(true);
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            if (webSocket != null) {
                webSocket.close(1000, "Client exit");
            }
            reportClosedSafe();
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
            // Report CONNECTED and start heartbeats
            try {
                postStatus("/api/tunnels/" + tunnelId + "/connected");
            } catch (final Exception e) {
                log.debug("Failed to report TCP connected: {}", e.toString());
            }
            // allow reporting CLOSED again for future disconnects after a successful reconnect
            closedReported.set(false);
            try {
                final var existing = heartbeatTask;
                if (existing != null && !existing.isCancelled()) {
                    existing.cancel(true);
                }
                heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                    try {
                        postStatus("/api/tunnels/" + tunnelId + "/heartbeat");
                    } catch (final Exception e) {
                        log.debug("TCP heartbeat failed: {}", e.toString());
                    }
                }, 0, 20, TimeUnit.SECONDS);
            } catch (final Exception e) {
                log.debug("Failed to start TCP heartbeat: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                final var message = MAPPER.readValue(text, WsTunnelMessage.class);
                handleControl(message);
            } catch (final Exception e) {
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
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            reportClosedSafe();
            closed.countDown();
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable throwable, final Response response) {
            log.warn("Tunnel failure: {}", throwable.toString());
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            reportClosedSafe();
            closed.countDown();
        }
    }

    private void reportClosedSafe() {
        if (closedReported.compareAndSet(false, true)) {
            try {
                postStatus("/api/tunnels/" + tunnelId + "/closed");
            } catch (final Exception e) {
                log.debug("Failed to report TCP closed: {}", e.toString());
            }
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
                webSocket.send(MAPPER.writeValueAsString(ack));
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
                    } catch (final Exception e) {
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
        } catch (final Exception e) {
            // ignore
        } finally {
            try {
                final var message = new WsTunnelMessage();
                message.setWsType(WsTunnelMessage.Type.CLOSE);
                message.setConnectionId(local.connectionId);
                webSocket.send(MAPPER.writeValueAsString(message));
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

    private void postStatus(final String path) throws Exception {
        final var base = (secure ? "https://" : "http://") + proxyHost + ":" + proxyHttpPort;
        final var url = base + path;
        final var body = RequestBody.create("{}", MediaType.parse("application/json"));
        final var builder = new Request.Builder().url(url).post(body);
        if (authToken != null && !authToken.isBlank()) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        try (final var response = rest.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                log.debug("Status POST failed {} {} for {}", response.code(), response.message(), path);
            }
        }
    }
}
