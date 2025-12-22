/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.tunnel;

import java.util.List;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * A permissive WebSocket HandshakeHandler that negotiates the subprotocol by simply
 * echoing back the first protocol requested by the client, if any. This is helpful
 * for clients (e.g., Vaadin) that expect the selected subprotocol to match their
 * request without the server advertising a fixed list.
 */
public class PermissiveSubprotocolHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected String selectProtocol(final List<String> requestedProtocols, final WebSocketHandler webSocketHandler) {
        return requestedProtocols.stream().findFirst().orElse(null);
    }
}
