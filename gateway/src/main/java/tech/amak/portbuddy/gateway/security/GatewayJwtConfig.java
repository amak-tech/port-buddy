package tech.amak.portbuddy.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class GatewayJwtConfig {

    @Value("${app.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        final var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        final var withIssuer = new JwtIssuerValidator(issuer);
        final var validator = new DelegatingOAuth2TokenValidator<Jwt>(new JwtTimestampValidator(), withIssuer);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
