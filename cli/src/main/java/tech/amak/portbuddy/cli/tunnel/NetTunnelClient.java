/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.cli.tunnel;

import static tech.amak.portbuddy.cli.utils.JsonUtils.MAPPER;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
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
import tech.amak.portbuddy.cli.ui.NetTrafficSink;
import tech.amak.portbuddy.cli.config.ConfigurationService;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.tunnel.BinaryWsFrame;
import tech.amak.portbuddy.common.tunnel.ControlMessage;
import tech.amak.portbuddy.common.tunnel.MessageEnvelope;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@RequiredArgsConstructor
public class NetTunnelClient {

    private final String proxyHost;
    private final int proxyHttpPort;
    /**
     * Whether the WebSocket should use TLS (wss). Must reflect the scheme of the configured server URL.
     */
    private final boolean secure;
    private final UUID tunnelId;
    private final String localHost;
    private final int localPort;
    private final TunnelType tunnelType;
    // Expected public connection details returned by the server during expose REST call
    private final String expectedPublicHost;
    private final int expectedPublicPort;
    private final String authToken; // Bearer token if available
    private final NetTrafficSink trafficSink;

    private final OkHttpClient http = new OkHttpClient();
    private final OkHttpClient rest = new OkHttpClient();
    private WebSocket webSocket;

    private final Map<String, LocalTcp> locals = new ConcurrentHashMap<>();
    private final Map<String, LocalUdp> udpLocals = new ConcurrentHashMap<>();
    private CountDownLatch closed = new CountDownLatch(1);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "pb-net-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile ScheduledFuture<?> wsHeartbeatTask;
    private final AtomicBoolean closedReported = new AtomicBoolean(false);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicBoolean warnedAboutReassignment = new AtomicBoolean(false);

    /**
     * Establishes and maintains a WebSocket connection for TCP/UDP tunneling.
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
                final var publicHostParam = (expectedPublicHost == null || expectedPublicHost.isBlank())
                    ? ""
                    : "&public-host=" + URLEncoder.encode(expectedPublicHost, StandardCharsets.UTF_8);
                final var path = "/api/net-tunnel/" + tunnelId
                                 + "?type=" + tunnelType.name().toLowerCase()
                                 + "&port=" + expectedPublicPort
                                 + publicHostParam;
                final var url = toWebSocketUrl(scheme + proxyHost + ":" + proxyHttpPort, path);
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
                log.info("Net tunnel disconnected; reconnecting in {} ms...", backoffMs);
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                log.warn("Net tunnel loop error: {}", e.toString());
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
            final var wsTask = wsHeartbeatTask;
            if (wsTask != null) {
                wsTask.cancel(true);
            }
            if (webSocket != null) {
                webSocket.close(1000, "Client exit");
            }
            reportClosedSafe();
        } catch (final Exception ignore) {
            log.debug("TCP tunnel close error: {}", ignore.toString());
        }
    }

    private void close(final LocalTcp localTcp) {
        if (localTcp != null) {
            try {
                localTcp.sock.close();
            } catch (final Exception e) {
                log.debug("Failed to close TCP: {}", e.toString());
            }
        }
    }

    private void close(final LocalUdp localUdp) {
        if (localUdp != null) {
            try {
                localUdp.sock.close();
            } catch (final Exception e) {
                log.debug("Failed to close UDP local: {}", e.toString());
            }
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
                log.debug("Failed to report NET connected: {}", e.toString());
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
                        log.debug("NET heartbeat failed: {}", e.toString());
                    }
                }, 0, 20, TimeUnit.SECONDS);
            } catch (final Exception e) {
                log.debug("Failed to start NET heartbeat: {}", e.toString());
            }

            // Start WS application-level heartbeat (PING/PONG)
            try {
                final var existingWs = wsHeartbeatTask;
                if (existingWs != null && !existingWs.isCancelled()) {
                    existingWs.cancel(true);
                }
                final var config = ConfigurationService.INSTANCE.getConfig();
                final var intervalSec = Math.max(1, config.getHealthcheckIntervalSec());
                wsHeartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                    try {
                        final var ping = new ControlMessage();
                        ping.setType(ControlMessage.Type.PING);
                        ping.setTs(System.currentTimeMillis());
                        NetTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(ping));
                    } catch (final Exception e) {
                        log.debug("WS heartbeat send failed: {}", e.toString());
                    }
                }, intervalSec, intervalSec, TimeUnit.SECONDS);
            } catch (final Exception e) {
                log.debug("Failed to start WS heartbeat: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                final var env = MAPPER.readValue(text, MessageEnvelope.class);
                if (env.getKind() != null && env.getKind().equals("CTRL")) {
                    // Ignore CTRL (e.g., PONG) messages
                    return;
                }
                if (env.getKind() != null && env.getKind().equals("WS")) {
                    final var msg = MAPPER.readValue(text, WsTunnelMessage.class);
                    handleControl(msg);
                    return;
                }
                // Unknown kinds are ignored for NET tunnels
            } catch (final Exception e) {
                log.warn("Failed to process WS text message: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final ByteString bytes) {
            try {
                final var decoded = BinaryWsFrame.decode(bytes.toByteArray());
                if (decoded == null) {
                    return;
                }
                if (tunnelType == TunnelType.TCP) {
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
                } else if (tunnelType == TunnelType.UDP) {
                    // For UDP, forward the datagram to local UDP server using per-connection socket
                    final var connId = decoded.connectionId();
                    var localUdp = udpLocals.get(connId);
                    if (localUdp == null) {
                        try {
                            final var sock = new DatagramSocket();
                            localUdp = new LocalUdp(connId, sock);
                            udpLocals.put(connId, localUdp);
                            // start receive loop for this connection
                            final var localUdpRef = localUdp;
                            new Thread(() -> pumpUdpLocalToProxy(localUdpRef)).start();
                        } catch (final Exception e) {
                            log.debug("Failed to create local UDP socket: {}", e.toString());
                            return;
                        }
                    }
                    try {
                        final var packet = new DatagramPacket(decoded.data(), decoded.data().length,
                            new InetSocketAddress(localHost, localPort));
                        localUdp.sock.send(packet);
                        if (trafficSink != null) {
                            trafficSink.onBytesIn(decoded.data().length);
                        }
                    } catch (final Exception e) {
                        log.debug("Write to local UDP failed: {}", e.toString());
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
            final var wsTask = wsHeartbeatTask;
            if (wsTask != null) {
                wsTask.cancel(true);
            }
            reportClosedSafe();
            closed.countDown();
            // Close UDP sockets
            if (tunnelType == TunnelType.UDP) {
                for (final var entry : udpLocals.entrySet()) {
                    close(entry.getValue());
                }
                udpLocals.clear();
            }
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable throwable, final Response response) {
            log.warn("Tunnel failure: {}", throwable.toString());
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            final var wsTask = wsHeartbeatTask;
            if (wsTask != null) {
                wsTask.cancel(true);
            }
            reportClosedSafe();
            closed.countDown();
            if (tunnelType == TunnelType.UDP) {
                for (final var entry : udpLocals.entrySet()) {
                    close(entry.getValue());
                }
                udpLocals.clear();
            }
        }
    }

    private void reportClosedSafe() {
        if (closedReported.compareAndSet(false, true)) {
            try {
                postStatus("/api/tunnels/" + tunnelId + "/closed");
            } catch (final Exception e) {
                log.debug("Failed to report NET closed: {}", e.toString());
            }
        }
    }

    private void handleControl(final WsTunnelMessage message) throws Exception {
        final var connId = message.getConnectionId();
        switch (message.getWsType()) {
            case EXPOSED -> {
                final var actualHost = message.getPublicHost();
                final var actualPort = message.getPublicPort();
                if (actualHost != null && actualPort != null) {
                    final var hostDiffers = expectedPublicHost != null && !expectedPublicHost.equals(actualHost);
                    final var portDiffers = expectedPublicPort != actualPort;
                    if ((hostDiffers || portDiffers) && warnedAboutReassignment.compareAndSet(false, true)) {
                        System.out.printf(
                            "Warning: requested public %s:%d but exposed on %s:%d%n",
                            expectedPublicHost,
                            expectedPublicPort,
                            actualHost,
                            actualPort
                        );
                    }
                }
            }
            case OPEN -> {
                if (tunnelType == TunnelType.TCP) {
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
                } else {
                    // UDP does not use OPEN for per-flow; ignore or acknowledge for compatibility
                    final var ack = new WsTunnelMessage();
                    ack.setWsType(WsTunnelMessage.Type.OPEN_OK);
                    ack.setConnectionId(connId);
                    webSocket.send(MAPPER.writeValueAsString(ack));
                }
            }
            case BINARY -> {
                if (tunnelType == TunnelType.TCP) {
                    // Base64 payload from proxy to local TCP (legacy)
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
                } else if (tunnelType == TunnelType.UDP) {
                    // Legacy TEXT BINARY for UDP: forward to local as datagram
                    if (message.getDataB64() != null) {
                        var localUdp = udpLocals.get(connId);
                        if (localUdp == null) {
                            final var sock = new DatagramSocket();
                            localUdp = new LocalUdp(connId, sock);
                            udpLocals.put(connId, localUdp);
                            final var localUdpRef = localUdp;
                            new Thread(() -> pumpUdpLocalToProxy(localUdpRef)).start();
                        }
                        final var bytes = Base64.getDecoder().decode(message.getDataB64());
                        final var packet = new DatagramPacket(bytes, bytes.length,
                            new InetSocketAddress(localHost, localPort));
                        localUdp.sock.send(packet);
                        if (trafficSink != null) {
                            trafficSink.onBytesIn(bytes.length);
                        }
                    }
                }
            }
            case CLOSE -> {
                if (tunnelType == TunnelType.TCP) {
                    close(locals.remove(connId));
                } else {
                    close(udpLocals.remove(connId));
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
            close(local);
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

    private static class LocalUdp {
        final String connectionId;
        final DatagramSocket sock;

        LocalUdp(final String connectionId, final DatagramSocket sock) {
            this.connectionId = connectionId;
            this.sock = sock;
        }
    }

    private void pumpUdpLocalToProxy(final LocalUdp local) {
        final var buffer = new byte[65535];
        try {
            while (!local.sock.isClosed()) {
                final var packet = new DatagramPacket(buffer, buffer.length);
                local.sock.receive(packet);
                final var frame = BinaryWsFrame
                    .encodeToArray(local.connectionId, packet.getData(), packet.getOffset(), packet.getLength());
                webSocket.send(ByteString.of(frame));
                if (trafficSink != null) {
                    trafficSink.onBytesOut(packet.getLength());
                }
            }
        } catch (final Exception e) {
            // ignore normal close
        } finally {
            close(local);
            udpLocals.remove(local.connectionId);
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
