package tech.amak.portbuddy.cli.tunnel;

import static tech.amak.portbuddy.cli.utils.JsonUtils.MAPPER;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
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
import tech.amak.portbuddy.cli.ui.HttpLogSink;
import tech.amak.portbuddy.common.tunnel.ControlMessage;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.MessageEnvelope;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@RequiredArgsConstructor
public class HttpTunnelClient {

    private final String serverUrl; // e.g. https://portbuddy.dev
    private final String tunnelId;
    private final String localHost;
    private final int localPort;
    private final String localScheme; // http or https
    private final String authToken; // Bearer token for API auth
    private final String publicBaseUrl; // e.g. https://abc123.portbuddy.dev
    private final HttpLogSink httpLogSink;

    // OkHttp client used exclusively for the control WebSocket connection to the server
    private final OkHttpClient http = new OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // keep-alive for WS
        .pingInterval(15, TimeUnit.SECONDS) // send pings to keep intermediaries/proxies from dropping idle WS
        .retryOnConnectionFailure(true)
        .build();
    // Separate OkHttp client for calling the local target service (avoid any interference with WS client)
    private final OkHttpClient localHttp = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build();

    private WebSocket webSocket;
    private CountDownLatch closed = new CountDownLatch(1);
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final var thread = new Thread(runnable, "port-buddy-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private volatile ScheduledFuture<?> heartbeatTask;
    private final ExecutorService requestExecutor = Executors.newFixedThreadPool(4, runnable -> {
        final var thread = new Thread(runnable, "port-buddy-http-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, WebSocket> localWebsocketMap = new ConcurrentHashMap<>();

    /**
     * Establishes and maintains a blocking WebSocket connection to the server.
     * This method constructs a WebSocket connection to a server using a URL
     * derived from the server's URL combined with the tunnel identifier. The
     * method blocks until the WebSocket connection is closed or interrupted.
     * Behavior:
     * - Converts the server URL and tunnel identifier into a WebSocket URL.
     * - Opens a WebSocket connection to the calculated URL and uses a
     * {@code Listener} to handle WebSocket events, such as incoming messages,
     * connection closure, or failures.
     * - Waits on the {@code closeLatch} to ensure blocking behavior until the
     * connection is terminated.
     * Exceptions:
     * - Catches and handles {@link InterruptedException} if the wait operation
     * on the latch is interrupted. Restores the interrupted thread state.
     */
    public void runBlocking() {
        var backoffMs = 1000L;
        final var maxBackoffMs = 30000L;
        while (!stop.get()) {
            try {
                closed = new CountDownLatch(1);
                final var wsUrl = toWebSocketUrl(serverUrl, "/api/http-tunnel/" + tunnelId);
                final var request = new Request.Builder().url(wsUrl);
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
                log.info("Tunnel disconnected; reconnecting in {} ms...", backoffMs);
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, maxBackoffMs);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (final Exception e) {
                log.warn("Tunnel loop error: {}", e.toString());
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
     * Closes the WebSocket connection associated with this HTTP tunnel client.
     * This method attempts to gracefully close the WebSocket connection, if it exists,
     * using the standard WebSocket closure status code 1000 (indicating a normal closure)
     * and a reason message "Client exit". If an exception occurs during the closure process,
     * it is logged at the debug level and suppressed to ensure that the exception does not
     * disrupt the application's flow.
     * Behavior:
     * - If there is an active WebSocket (represented by the {@code webSocket} field),
     * it calls the {@code close} method on the WebSocket instance, passing the closure
     * status code and reason message.
     * - Logs any exception encountered during the close operation at the debug level
     * without re-throwing it.
     * Thread-safety: This method is thread-safe, as it uses a local reference to the
     * {@code webSocket} field to prevent potential null pointer exceptions caused by
     * concurrent modifications.
     */
    public void close() {
        try {
            stop.set(true);
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            requestExecutor.shutdownNow();
            if (webSocket != null) {
                webSocket.close(1000, "Client exit");
                log.debug("Websocket closed: 1000 OK");
            }
        } catch (final Exception ignore) {
            log.debug("HTTP tunnel close error: {}", ignore.toString());
        }
    }

    private String toWebSocketUrl(String base, String path) {
        var uri = URI.create(base);
        var scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            scheme = "wss";
        } else if ("http".equalsIgnoreCase(scheme)) {
            scheme = "ws";
        }
        final var hostPort = (uri.getPort() == -1) ? uri.getHost() : (uri.getHost() + ":" + uri.getPort());
        return scheme + "://" + hostPort + path;
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onOpen(final WebSocket webSocket, final Response response) {
            log.debug("Tunnel connected to server");
            // Start application-level heartbeat PINGs
            try {
                if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
                    heartbeatTask.cancel(true);
                }
                heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                    try {
                        final var ping = new ControlMessage();
                        ping.setType(ControlMessage.Type.PING);
                        ping.setTs(System.currentTimeMillis());
                        HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(ping));
                    } catch (final Exception e) {
                        log.debug("Heartbeat send failed: {}", e.toString());
                    }
                }, 0, 20, TimeUnit.SECONDS);
            } catch (final Exception e) {
                log.debug("Failed to start heartbeat: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                log.debug("Received WS message: {}", text);
                final var env = MAPPER.readValue(text, MessageEnvelope.class);
                if (env.getKind() != null && env.getKind().equals("CTRL")) {
                    // Ignore control messages (e.g., PONG)
                    return;
                }
                if (env.getKind() != null && env.getKind().equals("WS")) {
                    final var wsMsg = MAPPER.readValue(text, WsTunnelMessage.class);
                    handleWsFromServer(wsMsg);
                    return;
                }
                final var message = MAPPER.readValue(text, HttpTunnelMessage.class);
                if (message.getType() == HttpTunnelMessage.Type.REQUEST) {
                    // Offload request processing to a worker thread to avoid blocking the WS listener
                    requestExecutor.submit(() -> {
                        try {
                            final var resp = handleRequest(message);
                            final var json = MAPPER.writeValueAsString(resp);
                            HttpTunnelClient.this.webSocket.send(json);
                            log.debug("Responded to WS request: {}", resp.getId());
                        } catch (final Exception ex) {
                            log.warn("Failed to handle tunneled request {}: {}", message.getId(), ex.toString());
                            try {
                                final var error = buildErrorMessage(message.getId(), 502, "Proxy error");
                                HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(error));
                            } catch (final Exception e) {
                                log.error("Failed to send error response: {}", e.getMessage(), e);
                            }
                        }
                    });
                } else {
                    log.debug("Ignoring non-REQUEST msg");
                }
            } catch (Exception e) {
                log.warn("Failed to process WS message: {}", e.toString());
            }
        }

        @Override
        public void onClosed(final WebSocket webSocket, final int code, final String reason) {
            log.info("Tunnel closed: {} {}", code, reason);
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            closed.countDown();
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable error, final Response response) {
            log.warn("Tunnel failure: {}", error.toString());
            final var task = heartbeatTask;
            if (task != null) {
                task.cancel(true);
            }
            closed.countDown();
        }
    }

    private void handleWsFromServer(final WsTunnelMessage message) {
        final var connId = message.getConnectionId();
        switch (message.getWsType()) {
            case OPEN -> {
                // Connect to local target via WS
                final var localWsScheme = "https".equalsIgnoreCase(localScheme) ? "wss" : "ws";
                var url = localWsScheme + "://" + localHost + ":" + localPort + (message.getPath() != null ? message.getPath() : "/");
                if (message.getQuery() != null && !message.getQuery().isBlank()) {
                    url += "?" + message.getQuery();
                }
                final var builder = new Request.Builder().url(url);
                if (message.getHeaders() != null) {
                    for (var entry : message.getHeaders().entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            builder.addHeader(entry.getKey(), entry.getValue());
                        }
                    }
                }
                final var local = http.newWebSocket(builder.build(), new LocalWsListener(connId));
                localWebsocketMap.put(connId, local);
            }
            case TEXT -> {
                final var local = localWebsocketMap.get(connId);
                if (local != null) {
                    local.send(message.getText() != null ? message.getText() : "");
                }
            }
            case BINARY -> {
                final var local = localWebsocketMap.get(connId);
                if (local != null && message.getDataB64() != null) {
                    local.send(ByteString.of(Base64.getDecoder().decode(message.getDataB64())));
                }
            }
            case CLOSE -> {
                final var local = localWebsocketMap.remove(connId);
                if (local != null) {
                    local.close(message.getCloseCode() != null ? message.getCloseCode() : 1000, message.getCloseReason());
                }
            }
            default -> {
            }
        }
    }

    private class LocalWsListener extends WebSocketListener {
        private final String connectionId;

        LocalWsListener(String connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        public void onOpen(final WebSocket webSocket, final Response response) {
            try {
                final var ack = new WsTunnelMessage();
                ack.setWsType(WsTunnelMessage.Type.OPEN_OK);
                ack.setConnectionId(connectionId);
                HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(ack));
            } catch (final Exception ignore) {
                log.error("Failed to send local WS open ack: {}", ignore.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                final var message = new WsTunnelMessage();
                message.setWsType(WsTunnelMessage.Type.TEXT);
                message.setConnectionId(connectionId);
                message.setText(text);
                HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(message));
            } catch (Exception e) {
                log.debug("Failed to forward local text WS: {}", e.toString());
            }
        }

        @Override
        public void onMessage(final WebSocket webSocket, final ByteString bytes) {
            try {
                final var message = new WsTunnelMessage();
                message.setWsType(WsTunnelMessage.Type.BINARY);
                message.setConnectionId(connectionId);
                message.setDataB64(Base64.getEncoder().encodeToString(bytes.toByteArray()));
                HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(message));
            } catch (Exception e) {
                log.debug("Failed to forward local binary WS: {}", e.toString());
            }
        }

        @Override
        public void onClosed(final WebSocket webSocket, final int code, final String reason) {
            try {
                localWebsocketMap.remove(connectionId);
                final var message = new WsTunnelMessage();
                message.setWsType(WsTunnelMessage.Type.CLOSE);
                message.setConnectionId(connectionId);
                message.setCloseCode(code);
                message.setCloseReason(reason);
                HttpTunnelClient.this.webSocket.send(MAPPER.writeValueAsString(message));
            } catch (Exception e) {
                log.debug("Failed to notify close: {}", e.toString());
            }
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable error, final Response response) {
            onClosed(webSocket, 1011, error.toString());
        }
    }

    private HttpTunnelMessage handleRequest(final HttpTunnelMessage requestMessage) {
        final var method = requestMessage.getMethod();
        var url = localScheme + "://" + localHost + ":" + localPort + requestMessage.getPath();
        if (requestMessage.getQuery() != null && !requestMessage.getQuery().isBlank()) {
            url += "?" + requestMessage.getQuery();
        }

        final var targetRequest = new Request.Builder()
            .url(url)
            .method(method, buildBody(method, requestMessage.getBodyB64(), requestMessage.getBodyContentType()));

        if (requestMessage.getHeaders() != null) {
            for (var header : requestMessage.getHeaders().entrySet()) {
                final var name = header.getKey();
                final var values = header.getValue();
                if (name == null || values == null) {
                    continue;
                }
                if (name.equalsIgnoreCase("Host")) {
                    continue; // Host will be set by client
                }
                if (name.equalsIgnoreCase("Content-Type")) {
                    // Content-Type is derived from RequestBody media type
                    continue;
                }
                for (var value : values) {
                    if (value != null) {
                        targetRequest.addHeader(name, value);
                    }
                }
            }
        }

        try (final var targetResponse = localHttp.newCall(targetRequest.build()).execute()) {
            final var successMessage = new HttpTunnelMessage();
            successMessage.setId(requestMessage.getId());
            successMessage.setType(HttpTunnelMessage.Type.RESPONSE);
            successMessage.setStatus(targetResponse.code());
            successMessage.setRespHeaders(extractHeaders(targetResponse));
            final var body = targetResponse.body();
            if (body != null) {
                final var bytes = body.bytes();
                if (bytes.length > 0) {
                    successMessage.setRespBodyB64(Base64.getEncoder().encodeToString(bytes));
                }
            }
            // Log to UI sink
            try {
                if (httpLogSink != null) {
                    var displayUrl = publicBaseUrl;
                    if (requestMessage.getPath() != null) {
                        displayUrl += requestMessage.getPath();
                    }
                    if (requestMessage.getQuery() != null && !requestMessage.getQuery().isBlank()) {
                        displayUrl += "?" + requestMessage.getQuery();
                    }
                    httpLogSink.onHttpLog(method, displayUrl, targetResponse.code());
                }
            } catch (final Exception ignore) {
                log.debug("HTTP log sink failed: {}", ignore.toString());
            }
            return successMessage;
        } catch (final Exception e) {
            final var errorMessage = buildErrorMessage(requestMessage.getId(), 502, "Bad Gateway: " + e.getMessage());
            try {
                if (httpLogSink != null) {
                    var displayUrl = publicBaseUrl;
                    if (requestMessage.getPath() != null) {
                        displayUrl += requestMessage.getPath();
                    }
                    if (requestMessage.getQuery() != null && !requestMessage.getQuery().isBlank()) {
                        displayUrl += "?" + requestMessage.getQuery();
                    }
                    httpLogSink.onHttpLog(method, displayUrl, 502);
                }
            } catch (final Exception ignore) {
                log.debug("HTTP log sink failed: {}", ignore.toString());
            }
            return errorMessage;
        }
    }

    private static HttpTunnelMessage buildErrorMessage(final String id, final int status, final String message) {
        final var error = new HttpTunnelMessage();
        error.setId(id);
        error.setType(HttpTunnelMessage.Type.RESPONSE);
        error.setStatus(status);
        final var headers = Map.<String, List<String>>of("Content-Type", List.of("text/plain; charset=utf-8"));
        error.setRespHeaders(headers);
        error.setRespBodyB64(Base64.getEncoder().encodeToString((message).getBytes(StandardCharsets.UTF_8)));

        return error;
    }

    private RequestBody buildBody(final String method, final String bodyB64, final String contentType) {
        // Methods that usually don't have body
        if (bodyB64 == null) {
            return methodSupportsBody(method) ? RequestBody.create(new byte[0], contentType != null ? MediaType.parse(contentType) : null) : null;
        }
        final var bytes = Base64.getDecoder().decode(bodyB64);
        final var mediaType = contentType != null && !contentType.isBlank() ? MediaType.parse(contentType) : MediaType.parse("application/octet-stream");
        return RequestBody.create(bytes, mediaType);
    }

    private boolean methodSupportsBody(final String method) {
        if (method == null) {
            return false;
        }
        return switch (method.toUpperCase()) {
            case "POST", "PUT", "PATCH" -> true;
            default -> false;
        };
    }

    private Map<String, List<String>> extractHeaders(final Response response) {
        final var map = new HashMap<String, List<String>>();
        for (final var name : response.headers().names()) {
            final var values = response.headers(name);
            if (values != null && !values.isEmpty()) {
                map.put(name, new ArrayList<>(values));
            }
        }
        return map;
    }
}
