/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.netproxy.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

/**
 * Configuration for JWT decoding using a load-balanced client.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final AppProperties appProperties;

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Creates a JwtDecoder that uses a load-balanced RestTemplate to fetch the JWK set.
     *
     * @param restTemplate the load-balanced RestTemplate
     * @return the JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder(final RestTemplate restTemplate) {
        final var decoder = NimbusJwtDecoder
            .withJwkSetUri(appProperties.jwt().jwkSetUri())
            .restOperations(restTemplate)
            .build();
        final var withIssuer = new JwtIssuerValidator(appProperties.jwt().issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), withIssuer
        ));
        return decoder;
    }
}
