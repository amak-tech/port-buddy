package tech.amak.portbuddy.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final RsaKeyProvider rsaKeyProvider;
    private final AppProperties properties;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        return rsaKeyProvider.jwkSource();
    }

    @Bean
    public JwtEncoder jwtEncoder(final JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(final JWKSource<SecurityContext> jwkSource) {
        final var jwtProcessor = new DefaultJWTProcessor<>();
        final var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        jwtProcessor.setJWSKeySelector(keySelector);
        final var decoder = new NimbusJwtDecoder(jwtProcessor);
        final var withIssuer = new JwtIssuerValidator(properties.jwt().issuer());
        final var validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), withIssuer);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
