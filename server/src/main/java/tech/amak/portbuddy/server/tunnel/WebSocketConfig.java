package tech.amak.portbuddy.server.tunnel;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TunnelWebSocketHandler tunnelWebSocketHandler;
    private final PublicWebSocketProxyHandler publicWebSocketProxyHandler;

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(tunnelWebSocketHandler, "/api/tunnel/{tunnelId}")
            .setAllowedOrigins("*");
        // Public WS endpoint for tunneled hosts (matches any path)
        registry.addHandler(publicWebSocketProxyHandler, "/**")
            .setAllowedOrigins("*");
    }
}
