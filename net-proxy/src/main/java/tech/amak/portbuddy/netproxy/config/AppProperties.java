/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.netproxy.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String publicHost,
    WebSocket webSocket,
    Jwt jwt
) {

    public record WebSocket(
        DataSize maxTextMessageSize,
        DataSize maxBinaryMessageSize,
        Duration sessionIdleTimeout
    ) {
    }

    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }
}
