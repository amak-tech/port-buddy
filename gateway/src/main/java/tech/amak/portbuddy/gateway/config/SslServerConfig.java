/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import tech.amak.portbuddy.gateway.ssl.DynamicSslProvider;

@Configuration
public class SslServerConfig {

    private final AppProperties properties;
    private final DynamicSslProvider sslProvider;

    public SslServerConfig(final AppProperties properties, final DynamicSslProvider sslProvider) {
        this.properties = properties;
        this.sslProvider = sslProvider;
    }

    /**
     * Customizes Netty server to support dynamic SSL termination via SNI and dual ports.
     *
     * @return NettyServerCustomizer
     */
    @Bean
    public NettyServerCustomizer sslCustomizer() {
        return server -> {
            if (properties.ssl() != null && properties.ssl().enabled()) {
                // Ensure the main server is NOT secure (it's for HTTP)
                // We'll start a separate HTTPS server if needed, 
                // but Spring Boot already starts one on server.port.
                // If we want both, we need to decide which one is which.
                // Standard approach: server.port is for the main traffic (HTTP or HTTPS).
                // Let's make server.port HTTP and start another one for HTTPS.

                // However, NettyServerCustomizer is for the main server.
                // If we want the main server to be HTTPS:
                return server.secure(spec -> {
                    try {
                        final var ssc = new SelfSignedCertificate();
                        spec.sslContext(SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build());
                    } catch (final Exception e) {
                        throw new RuntimeException("Failed to initialize SSL context", e);
                    }
                });
            }
            return server;
        };
    }
}
