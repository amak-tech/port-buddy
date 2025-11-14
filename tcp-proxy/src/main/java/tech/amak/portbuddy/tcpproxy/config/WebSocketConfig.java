package tech.amak.portbuddy.tcpproxy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.tcpproxy.tunnel.TcpTunnelWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TcpTunnelWebSocketHandler tcpHandler;

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        registry.addHandler(tcpHandler, "/api/tcp-tunnel/{tunnelId}").setAllowedOrigins("*");
    }
}
