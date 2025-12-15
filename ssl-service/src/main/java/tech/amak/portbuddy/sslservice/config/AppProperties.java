/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Jwt jwt,
    Acme acme,
    Storage storage,
    Mail mail
) {
    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }

    public record Acme(
        String serverUrl,
        String accountKeyPath,
        String contactEmail,
        String accountLocation,
        Retry retry
    ) {
    }

    public record Retry(
        int maxAttempts,
        long initialDelayMs,
        long maxDelayMs,
        double multiplier,
        long jitterMs
    ) {
    }

    public record Storage(
        String certificatesDir
    ) {
    }

    public record Mail(
        boolean enabled,
        String from,
        String defaultTo
    ) {
    }
}
