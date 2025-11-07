package tech.amak.portbuddy.server.tunnel;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;

/**
 * Registry of active HTTP tunnels and WS connections.
 */
@Slf4j
@Component
public class TunnelRegistry {

    private final Map<String, Tunnel> bySubdomain = new ConcurrentHashMap<>();
    private final Map<String, Tunnel> byTunnelId = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public Tunnel createPending(final String subdomain, final String tunnelId) {
        final var tunnel = new Tunnel(subdomain, tunnelId);
        bySubdomain.put(subdomain, tunnel);
        byTunnelId.put(tunnelId, tunnel);
        return tunnel;
    }

    public void remove(final Tunnel tunnel) {
        bySubdomain.remove(tunnel.subdomain());
        byTunnelId.remove(tunnel.tunnelId());
    }

    public Tunnel getBySubdomain(final String subdomain) {
        return bySubdomain.get(subdomain);
    }

    public Tunnel getByTunnelId(final String tunnelId) {
        return byTunnelId.get(tunnelId);
    }

    public boolean attachSession(final String tunnelId, final WebSocketSession session) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return false;
        }
        tunnel.setSession(session);
        return true;
    }

    public CompletableFuture<HttpTunnelMessage> forwardRequest(final String subdomain,
                                                               final HttpTunnelMessage request,
                                                               final Duration timeout) {
        final var tunnel = bySubdomain.get(subdomain);
        if (tunnel == null || !tunnel.isOpen()) {
            final var future = new CompletableFuture<HttpTunnelMessage>();
            future.completeExceptionally(new IllegalStateException("Tunnel not connected"));
            return future;
        }
        // Assign id if missing
        if (request.getId() == null) {
            request.setId(UUID.randomUUID().toString());
        }
        request.setType(HttpTunnelMessage.Type.REQUEST);
        final var future = new CompletableFuture<HttpTunnelMessage>();
        tunnel.pending().put(request.getId(), future);
        try {
            final var json = mapper.writeValueAsString(request);
            tunnel.session().sendMessage(new TextMessage(json));
        } catch (IOException e) {
            tunnel.pending().remove(request.getId());
            future.completeExceptionally(e);
            return future;
        }

        final var futureTimeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        // Apply timeout
        return future.orTimeout(futureTimeout.toMillis(), TimeUnit.MILLISECONDS)
            .whenComplete((res, err) ->
                tunnel.pending().remove(request.getId()));
    }

    public void onResponse(final String tunnelId, final HttpTunnelMessage response) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        final var future = tunnel.pending()
            .get(response.getId());
        if (future != null) {
            future.complete(response);
        }
    }

    // ============ WebSocket tunneling support ============
    public void sendWsToClient(final String tunnelId, final WsTunnelMessage message) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null || !tunnel.isOpen()) {
            return;
        }
        try {
            final var json = mapper.writeValueAsString(message);
            tunnel.session().sendMessage(new TextMessage(json));
        } catch (final IOException e) {
            log.warn("Failed to send WS message to client: {}", e.toString());
        }
    }

    public void registerBrowserWs(final String tunnelId,
                                  final String connectionId,
                                  final WebSocketSession browserSession) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        tunnel.browserByConnection().put(connectionId, browserSession);
        tunnel.browserReverse().put(browserSession, new Ids(tunnelId, connectionId));
    }

    public Ids unregisterBrowserWs(final WebSocketSession browserSession) {
        for (final var tunnel : byTunnelId.values()) {
            final var ids = tunnel.browserReverse().remove(browserSession);
            if (ids != null) {
                tunnel.browserByConnection().remove(ids.connectionId);
                return ids;
            }
        }
        return null;
    }

    public Ids findIdsByBrowserSession(final WebSocketSession browserSession) {
        for (final var tunnel : byTunnelId.values()) {
            final var ids = tunnel.browserReverse().get(browserSession);
            if (ids != null) {
                return ids;
            }
        }
        return null;
    }

    public WebSocketSession getBrowserSession(final String tunnelId, final String connectionId) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return null;
        }
        return tunnel.browserByConnection().get(connectionId);
    }

    @Data
    @AllArgsConstructor
    public static class Ids {
        private String tunnelId;
        private String connectionId;
    }

    public static class Tunnel {
        private final String subdomain;
        private final String tunnelId;
        @Setter
        private volatile WebSocketSession session;
        private final Map<String, CompletableFuture<HttpTunnelMessage>> pending = new ConcurrentHashMap<>();
        // Browser WS peers for this tunnel
        private final Map<String, WebSocketSession> browserByConnection = new ConcurrentHashMap<>();
        private final Map<WebSocketSession, Ids> browserReverse = new ConcurrentHashMap<>();

        public Tunnel(String subdomain, String tunnelId) {
            this.subdomain = subdomain;
            this.tunnelId = tunnelId;
        }

        public String subdomain() {
            return subdomain;
        }

        public String tunnelId() {
            return tunnelId;
        }

        public WebSocketSession session() {
            return session;
        }

        public Map<String, CompletableFuture<HttpTunnelMessage>> pending() {
            return pending;
        }

        public boolean isOpen() {
            return session != null && session.isOpen();
        }

        public Map<String, WebSocketSession> browserByConnection() {
            return browserByConnection;
        }

        public Map<WebSocketSession, Ids> browserReverse() {
            return browserReverse;
        }
    }
}
