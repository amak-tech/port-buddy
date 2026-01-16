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
