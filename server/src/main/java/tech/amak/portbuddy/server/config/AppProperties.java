/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Gateway gateway,
    WebSocket webSocket,
    Jwt jwt,
    Mail mail,
    Cli cli,
    PortReservations portReservations
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
    ) {
    }

    public record Jwt(
        String issuer,
        Duration ttl,
        Rsa rsa
    ) {
        public record Rsa(
            String currentKeyId,
            List<RsaKey> keys
        ) {
        }

        public record RsaKey(
            String id,
            Resource publicKeyPem,
            Resource privateKeyPem
        ) {
        }
    }

    public record Mail(
        String fromAddress,
        String fromName
    ) {
    }

    public record PortReservations(
        Range range
    ) {
        public record Range(
            int min,
            int max
        ) {
        }
    }

    public record Cli(
        String minVersion
    ) {
    }
}
