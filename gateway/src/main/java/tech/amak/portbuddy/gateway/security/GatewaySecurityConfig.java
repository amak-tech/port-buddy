/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .authorizeExchange(exchange -> exchange
                // Public endpoints (static, SPA, OAuth callbacks, JWKS, etc.)
                .pathMatchers(
                    "/", "/index.html", "/assets/**", "/favicon.*",
                    "/install", "/app/**", "/login/**", "/auth/callback",
                    "/oauth2/**", "/login/oauth2/**",
                    "/.well-known/jwks.json",
                    // Token exchange must be public to let CLI obtain a JWT
                    "/api/auth/token-exchange", "/api/auth/login", "/api/auth/register"
                    ).permitAll()
                // Secure API endpoints
                .pathMatchers("/api/**").authenticated()
                // Everything else is allowed (e.g., subdomain ingress and public tunnels)
                .anyExchange().permitAll()
            )
            // Validate bearer tokens for secured endpoints
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
