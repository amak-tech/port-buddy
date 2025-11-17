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
        registry.addHandler(tunnelWebSocketHandler, "/api/tunnel/{tunnelId}")
            .setAllowedOrigins("*");
        // Public WS endpoint for tunneled hosts (matches any path)
        registry.addHandler(publicWebSocketProxyHandler, "/**")
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
        container.setMaxTextMessageBufferSize((int) properties.webSocket().maxTextMessageSize().toBytes());
        container.setMaxBinaryMessageBufferSize((int) properties.webSocket().maxBinaryMessageSize().toBytes());
        return container;
    }
}
