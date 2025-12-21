/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.gateway.config.AppProperties;

@Configuration
@RequiredArgsConstructor
public class GatewayJwtConfig {

    private final WebClient.Builder loadBalancedWebClientBuilder;
    private final AppProperties properties;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        final var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwt().jwkSetUri())
            .webClient(loadBalancedWebClientBuilder.build())
            .build();
        final var withIssuer = new JwtIssuerValidator(properties.jwt().issuer());
        final var validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), withIssuer);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
