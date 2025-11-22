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
            .setAllowedOrigins("*");
        // Public WS endpoint for tunneled hosts (dedicated base path to avoid MVC collisions)
        registry.addHandler(publicWebSocketProxyHandler, "/_ws/**")
            .setAllowedOrigins("*");
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
