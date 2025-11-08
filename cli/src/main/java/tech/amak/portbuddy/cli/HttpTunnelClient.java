package tech.amak.portbuddy.cli;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

@Slf4j
@RequiredArgsConstructor
public class HttpTunnelClient {

    private final String serverUrl; // e.g. https://api.port-buddy.com
    private final String tunnelId;
    private final String localHost;
    private final int localPort;

    private final OkHttpClient http = new OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // keep-alive for WS
        .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private WebSocket webSocket;
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    private final Map<String, WebSocket> localWebsocketMap = new ConcurrentHashMap<>();

    /**
     * Establishes and maintains a blocking WebSocket connection to the server.
     *
     * <p>
     * This method constructs a WebSocket connection to a server using a URL
     * derived from the server's URL combined with the tunnel identifier. The
     * method blocks until the WebSocket connection is closed or interrupted.
     *
     * <p>
     * Behavior:
     * - Converts the server URL and tunnel identifier into a WebSocket URL.
     * - Opens a WebSocket connection to the calculated URL and uses a
     * {@code Listener} to handle WebSocket events, such as incoming messages,
     * connection closure, or failures.
     * - Waits on the {@code closeLatch} to ensure blocking behavior until the
     * connection is terminated.
     *
     * <p>
     * Exceptions:
     * - Catches and handles {@link InterruptedException} if the wait operation
     * on the latch is interrupted. Restores the interrupted thread state.
     */
    public void runBlocking() {
        final var wsUrl = toWebSocketUrl(serverUrl, "/api/tunnel/" + tunnelId);
        final var req = new Request.Builder().url(wsUrl).build();
        webSocket = http.newWebSocket(req, new Listener());

        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String toWebSocketUrl(String base, String path) {
        var u = URI.create(base);
        var scheme = u.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            scheme = "wss";
        } else if ("http".equalsIgnoreCase(scheme)) {
            scheme = "ws";
        }
        final var hostPort = (u.getPort() == -1) ? u.getHost() : (u.getHost() + ":" + u.getPort());
        return scheme + "://" + hostPort + path;
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onOpen(final WebSocket webSocket, final Response response) {
            log.info("Tunnel connected to server");
        }

        @Override
        public void onMessage(final WebSocket webSocket, final String text) {
            try {
                final JsonNode node = mapper.readTree(text);
                if (node.has("kind") && "WS".equals(node.get("kind").asText())) {
                    final var wsMsg = mapper.treeToValue(node, WsTunnelMessage.class);
                    handleWsFromServer(wsMsg);
                    return;
                }
                final var msg = mapper.treeToValue(node, HttpTunnelMessage.class);
                if (msg.getType() == HttpTunnelMessage.Type.REQUEST) {
                    final var resp = handleRequest(msg);
                    final var json = mapper.writeValueAsString(resp);
                    webSocket.send(json);
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
            closeLatch.countDown();
        }

        @Override
        public void onFailure(final WebSocket webSocket, final Throwable error, final Response response) {
            log.warn("Tunnel failure: {}", error.toString());
            closeLatch.countDown();
        }
    }

    private void handleWsFromServer(final WsTunnelMessage message) {
        final var connId = message.getConnectionId();
        switch (message.getWsType()) {
            case OPEN -> {
                // Connect to local target via WS
                var url = "ws://" + localHost + ":" + localPort + (message.getPath() != null ? message.getPath() : "/");
                if (message.getQuery() != null && !message.getQuery().isBlank()) {
                    url += "?" + message.getQuery();
                }
                final var builder = new Request.Builder().url(url);
                if (message.getHeaders() != null) {
                    for (var e : message.getHeaders().entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            builder.addHeader(e.getKey(), e.getValue());
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
        public void onOpen(WebSocket webSocket, Response response) {
            try {
                final var ack = new WsTunnelMessage();
                ack.setWsType(WsTunnelMessage.Type.OPEN_OK);
                ack.setConnectionId(connectionId);
                HttpTunnelClient.this.webSocket.send(mapper.writeValueAsString(ack));
            } catch (final Exception ignore) {
                log.error("Failed to send local WS open ack: {}", ignore.toString());
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                final var m = new WsTunnelMessage();
                m.setWsType(WsTunnelMessage.Type.TEXT);
                m.setConnectionId(connectionId);
                m.setText(text);
                HttpTunnelClient.this.webSocket.send(mapper.writeValueAsString(m));
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
                HttpTunnelClient.this.webSocket.send(mapper.writeValueAsString(message));
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
                HttpTunnelClient.this.webSocket.send(mapper.writeValueAsString(message));
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
        var url = "http://" + localHost + ":" + localPort + requestMessage.getPath();
        if (requestMessage.getQuery() != null && !requestMessage.getQuery().isBlank()) {
            url += "?" + requestMessage.getQuery();
        }

        final var targetRequest = new Request.Builder().url(url).method(method, buildBody(method, requestMessage.getBodyB64()));

        if (requestMessage.getHeaders() != null) {
            for (var header : requestMessage.getHeaders().entrySet()) {
                final var name = header.getKey();
                final var value = header.getValue();
                if (name == null || value == null) {
                    continue;
                }
                if (name.equalsIgnoreCase("Host")) {
                    continue; // Host will be set by client
                }
                targetRequest.addHeader(name, value);
            }
        }

        try (var targetResponse = http.newCall(targetRequest.build()).execute()) {
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
            return successMessage;
        } catch (final IOException e) {
            final var errorMessage = new HttpTunnelMessage();
            errorMessage.setId(requestMessage.getId());
            errorMessage.setType(HttpTunnelMessage.Type.RESPONSE);
            errorMessage.setStatus(502);
            final var headers = new HashMap<String, String>();
            headers.put("Content-Type", "text/plain; charset=utf-8");
            errorMessage.setRespHeaders(headers);
            errorMessage.setRespBodyB64(Base64.getEncoder().encodeToString(("Bad Gateway: " + e.getMessage()).getBytes(StandardCharsets.UTF_8)));
            return errorMessage;
        }
    }

    private RequestBody buildBody(final String method, final String bodyB64) {
        // Methods that usually don't have body
        if (bodyB64 == null) {
            return methodSupportsBody(method) ? RequestBody.create(new byte[0], null) : null;
        }
        final var bytes = Base64.getDecoder().decode(bodyB64);
        return RequestBody.create(bytes, MediaType.parse("application/octet-stream"));
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

    private Map<String, String> extractHeaders(final Response response) {
        final var map = new HashMap<String, String>();
        for (final var name : response.headers().names()) {
            map.put(name, response.header(name));
        }
        return map;
    }
}
