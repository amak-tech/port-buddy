package tech.amak.portbuddy.server.security;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final RsaKeyProvider rsaKeyProvider;

    /**
     * Creates a JWT token using the specified claims and subject. The generated token includes
     * details such as issuer, issued time, expiration time, subject, and any custom claims provided.
     *
     * @param claims  a map containing custom claims to include in the token. Can be null if no custom claims are
     *                required.
     * @param subject the subject of the token, typically representing the identity of the authenticated user.
     * @return a string representation of the generated JWT token.
     */
    public String createToken(final Map<String, Object> claims, final String subject) {
        final var now = Instant.now();
        final var ttl = properties.jwt().ttl();
        final var expiresAt = ttl == null ? now.plusSeconds(3600) : now.plus(ttl);

        final var builder = JwtClaimsSet.builder()
            .issuer(properties.jwt().issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(subject);
        if (claims != null) {
            for (final var entry : claims.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }
        }
        final var header = JwsHeader.with(SignatureAlgorithm.RS256)
            .type("JWT")
            .keyId(rsaKeyProvider.getCurrentKid())
            .build();
        final var jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build()));
        return jwt.getTokenValue();
    }
}
