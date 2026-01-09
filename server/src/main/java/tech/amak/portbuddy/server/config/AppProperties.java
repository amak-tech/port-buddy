/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

import tech.amak.portbuddy.common.Plan;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Gateway gateway,
    WebSocket webSocket,
    Jwt jwt,
    Mail mail,
    Cli cli,
    PortReservations portReservations,
    Subscriptions subscriptions,
    Stripe stripe
) {
    public record Subscriptions(
        Duration gracePeriod,
        Duration checkInterval,
        Tunnels tunnels
    ) {
        public record Tunnels(Map<Plan, Integer> base, Map<Plan, Integer> increment) {
        }
    }

    public record Gateway(
        String url,
        String domain,
        String subdomainUrlTemplate,
        String notFoundPage,
        String passcodePage
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

    public record Stripe(
        String webhookSecret,
        String apiKey,
        PriceIds priceIds
    ) {
        public record PriceIds(
            String pro,
            String team,
            String extraTunnel
        ) {
        }
    }
}
