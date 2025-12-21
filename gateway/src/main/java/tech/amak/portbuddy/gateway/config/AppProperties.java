/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String domain,
    String scheme,
    String url,
    String serverErrorPage,
    Jwt jwt,
    Ssl ssl
) {

    public record Ssl(
        int port,
        boolean enabled
    ) {
        public Ssl {
            if (port == 0) {
                port = 443;
            }
        }
    }


    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }

}
