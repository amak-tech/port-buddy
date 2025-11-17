package tech.amak.portbuddy.tcpproxy.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String publicHost,
    WebSocket webSocket
) {

    public record WebSocket(
        DataSize maxTextMessageSize,
        DataSize maxBinaryMessageSize,
        Duration sessionIdleTimeout
    ) {
    }
}
