package tech.amak.portbuddy.server.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Gateway gateway,
    WebSocket webSocket
) {
    public record Gateway(
        String url,
        String domain,
        String schema
    ) {
        public String subdomainHost() {
            return "." + domain;
        }
    }

    public record WebSocket(
        DataSize maxTextMessageSize,
        DataSize maxBinaryMessageSize,
        Duration sessionIdleTimeout
    ) {}
}
