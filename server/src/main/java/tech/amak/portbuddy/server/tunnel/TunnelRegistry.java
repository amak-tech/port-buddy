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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.common.tunnel.WsTunnelMessage;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;

/**
 * Registry of active HTTP tunnels and WS connections.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelRegistry {

    private final Map<String, Tunnel> bySubdomain = new ConcurrentHashMap<>();
    private final Map<UUID, Tunnel> byTunnelId = new ConcurrentHashMap<>();
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper mapper;

    /**
     * Registers a WebSocket session for a given tunnel entity by associating it with a newly created
     * tunnel instance based on the subdomain and tunnel ID.
     *
     * @param tunnelEntity the {@code TunnelEntity} containing information about the domain and tunnel identifiers
     * @param session      the {@code WebSocketSession} to be associated with the created tunnel instance
     * @return {@code true} to indicate successful registration
     */
    public boolean register(final TunnelEntity tunnelEntity, final WebSocketSession session) {
        final var tunnel = register(tunnelEntity.getDomain().getSubdomain(), tunnelEntity.getId(),
                tunnelEntity.getAccountId());
        tunnel.setSession(session);
        log.info("Registered tunnel {} with session {}", tunnel.tunnelId(), session.getId());
        return true;
    }

    /**
     * Creates a new pending Tunnel instance with the specified subdomain and tunnel ID
     * and registers it in the internal mappings.
     *
     * @param subdomain the subdomain associated with the tunnel
     * @param tunnelId  the unique identifier for the tunnel
     * @param accountId the account identifier for the tunnel
     * @return the created Tunnel instance
     */
    private Tunnel register(final String subdomain, final UUID tunnelId, final UUID accountId) {
        final var tunnel = new Tunnel(tunnelId, accountId);
        bySubdomain.put(subdomain, tunnel);
        byTunnelId.put(tunnelId, tunnel);
        return tunnel;
    }


    public Tunnel getBySubdomain(final String subdomain) {
        return bySubdomain.get(subdomain);
    }

    public Tunnel getByTunnelId(final UUID tunnelId) {
        return byTunnelId.get(tunnelId);
    }

    /**
     * Forwards an HTTP tunnel request through a WebSocket session associated with a specified subdomain.
     * If the tunnel is not connected or not open, the request will fail with an exception.
     * A timeout can be specified to limit the operation’s duration.
     *
     * @param subdomain the subdomain associated with the destination tunnel
     * @param request   the HTTP tunnel message to be forwarded
     * @param timeout   the maximum duration to wait for a response; null indicates default timeout
     * @return a CompletableFuture that will complete with the response message or fail with an exception
     */
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
            log.trace("Forwarded request {} to tunnel {}", json, tunnel.tunnelId());
        } catch (final IOException e) {
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

    /**
     * Processes an HTTP tunnel response message associated with the specified tunnel ID.
     * If the tunnel with the given ID exists and the response matches an existing pending
     * request in the tunnel, the request's future is completed with the response.
     *
     * @param tunnelId the unique identifier of the tunnel associated with the response
     * @param response the HTTP tunnel message representing the response to be processed
     */
    public void onResponse(final UUID tunnelId, final HttpTunnelMessage response) {
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

    /**
     * Sends a WebSocket message to the client associated with the specified tunnel.
     * If the specified tunnel is not open or does not exist, the operation is aborted.
     *
     * @param tunnelId the unique identifier of the tunnel to send the message to
     * @param message  the WebSocket message to be sent to the client
     */
    // ============ WebSocket tunneling support ============
    public void sendWsToClient(final UUID tunnelId, final WsTunnelMessage message) {
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

    /**
     * Registers a browser WebSocket session associated with the specified tunnel ID and connection ID.
     * If no tunnel with the provided tunnel ID exists, the operation is aborted.
     * The session is mapped in both the forward and reverse lookup structures for later reference.
     *
     * @param tunnelId       the unique identifier of the tunnel to associate with the browser session
     * @param connectionId   the unique identifier of the connection within the tunnel
     * @param browserSession the WebSocket session representing the browser connection
     */
    public void registerBrowserWs(final UUID tunnelId,
                                  final String connectionId,
                                  final WebSocketSession browserSession) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return;
        }
        tunnel.browserByConnection().put(connectionId, browserSession);
        tunnel.browserReverse().put(browserSession, new Ids(tunnelId, connectionId));
    }

    /**
     * Unregisters a browser WebSocket session from the tunnel registry. This method
     * removes the browser session from the reverse mapping and connection ID mapping
     * of the associated tunnel. If the session is successfully unregistered, the related
     * IDs (tunnel ID and connection ID) are returned; otherwise, null is returned.
     *
     * @param browserSession the WebSocketSession representing the browser connection to be unregistered
     * @return an {@code Ids} object containing the tunnel ID and connection ID associated with the
     *     unregistered browser session, or {@code null} if the session was not found
     */
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

    /**
     * Retrieves the tunnel and connection IDs associated with a given browser WebSocket session.
     * Iterates through the registered tunnels to find a reverse mapping for the specified session.
     *
     * @param browserSession the WebSocketSession representing the browser connection to look up
     * @return an {@code Ids} object containing the tunnel ID and connection ID associated with
     *     the specified session, or {@code null} if no match is found
     */
    public Ids findIdsByBrowserSession(final WebSocketSession browserSession) {
        for (final var tunnel : byTunnelId.values()) {
            final var ids = tunnel.browserReverse().get(browserSession);
            if (ids != null) {
                return ids;
            }
        }
        return null;
    }

    /**
     * Retrieves a WebSocket session associated with the specified tunnel ID and connection ID.
     * If no tunnel exists for the given tunnel ID or no session is associated with the
     * provided connection ID, this method returns {@code null}.
     *
     * @param tunnelId     the unique identifier of the tunnel from which to retrieve the session
     * @param connectionId the unique identifier of the connection within the tunnel
     * @return the WebSocketSession associated with the specified tunnel ID and connection ID,
     *     or {@code null} if no matching session is found
     */
    public WebSocketSession getBrowserSession(final UUID tunnelId, final String connectionId) {
        final var tunnel = byTunnelId.get(tunnelId);
        if (tunnel == null) {
            return null;
        }
        return tunnel.browserByConnection().get(connectionId);
    }

    @Data
    @AllArgsConstructor
    public static class Ids {
        private UUID tunnelId;
        private String connectionId;
    }

    @RequiredArgsConstructor
    public static class Tunnel {

        private final UUID tunnelId;
        private final UUID accountId;

        @Setter
        private volatile WebSocketSession session;
        private final Map<String, CompletableFuture<HttpTunnelMessage>> pending = new ConcurrentHashMap<>();
        // Browser WS peers for this tunnel
        private final Map<String, WebSocketSession> browserByConnection = new ConcurrentHashMap<>();
        private final Map<WebSocketSession, Ids> browserReverse = new ConcurrentHashMap<>();


        public UUID tunnelId() {
            return tunnelId;
        }

        public UUID accountId() {
            return accountId;
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

        // No passcode kept in-memory; use DB via TunnelService when needed
    }
}
