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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TunnelWebSocketHandler tunnelWebSocketHandler;
    private final PublicWebSocketProxyHandler publicWebSocketProxyHandler;
    private final AppProperties properties;

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(tunnelWebSocketHandler, "/api/http-tunnel/{tunnelId}")
            .setAllowedOrigins("*")
            // Echo back any requested subprotocol (some clients require it, e.g., Vaadin)
            .setHandshakeHandler(new PermissiveSubprotocolHandshakeHandler());
        // Public WS endpoint for tunneled hosts (dedicated base path to avoid MVC collisions)
        registry.addHandler(publicWebSocketProxyHandler, "/_ws/**")
            .setAllowedOrigins("*")
            // Echo back any requested subprotocol
            .setHandshakeHandler(new PermissiveSubprotocolHandshakeHandler());
    }

    /**
     * Configure the underlying servlet WebSocket container to allow larger text and
     * binary messages. We increase limits to 2 MiB to support larger tunneled
     * payloads between the CLI and the server.
     */
    @Bean
    public ServletServerContainerFactoryBean websocketContainer() {
        final var container = new ServletServerContainerFactoryBean();
        final var webSocket = properties.webSocket();
        container.setMaxTextMessageBufferSize((int) webSocket.maxTextMessageSize().toBytes());
        container.setMaxBinaryMessageBufferSize((int) webSocket.maxBinaryMessageSize().toBytes());
        // Prevent premature session termination by increasing idle timeout
        if (webSocket.sessionIdleTimeout() != null) {
            container.setMaxSessionIdleTimeout(webSocket.sessionIdleTimeout().toMillis());
        }
        return container;
    }

}
