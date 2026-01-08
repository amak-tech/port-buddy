/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String host,
    int port,
    int httpPort,
    String portPart,
    String domain,
    String scheme,
    String url,
    String serverErrorPage,
    Jwt jwt,
    Ssl ssl
) {

    public record Ssl(
        boolean enabled,
        Certificate fallback
    ) {
    }

    public record Certificate(
        boolean enabled,
        Resource keyCertChainFile,
        Resource keyFile
    ) {
    }

    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }

}
