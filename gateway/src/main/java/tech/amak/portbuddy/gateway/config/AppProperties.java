/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

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
        boolean enabled,
        Certificate fallback
    ) {
        /**
         * Constructs an SSL configuration record that ensures the default port value is set to 443
         * if the provided port value is zero.
         *
         * @param port     The port number for SSL connections. If set to 0, it defaults to 443.
         * @param enabled  A flag indicating if SSL is enabled.
         * @param fallback The fallback SSL certificate to be used when no specific certificate is available.
         */
        public Ssl {
            if (port == 0) {
                port = 443;
            }
        }
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
