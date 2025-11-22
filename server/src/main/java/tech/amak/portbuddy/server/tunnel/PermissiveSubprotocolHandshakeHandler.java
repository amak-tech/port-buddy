package tech.amak.portbuddy.server.tunnel;

import java.util.List;

import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * A permissive WebSocket HandshakeHandler that negotiates the subprotocol by simply
 * echoing back the first protocol requested by the client, if any. This is helpful
 * for clients (e.g., Vaadin) that expect the selected subprotocol to match their
 * request without the server advertising a fixed list.
 */
public class PermissiveSubprotocolHandshakeHandler extends DefaultHandshakeHandler {

    // Intentionally not annotating with @Override to avoid tight coupling to
    // Spring's internal method signature across versions.
    protected String determineNegotiatedSubprotocol(final List<String> requestedProtocols,
                                                    final List<String> supportedProtocols) {
        if (requestedProtocols != null && !requestedProtocols.isEmpty()) {
            // Echo the first requested subprotocol
            return requestedProtocols.get(0);
        }
        return null;
    }
}
