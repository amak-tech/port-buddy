/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.security;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.Role;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String TOKEN_TYPE = "JWT";

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
        return createToken(claims, subject, null);
    }

    /**
     * Creates a JWT token using the specified claims and subject. The generated token includes
     * details such as issuer, issued time, expiration time, subject, and any custom claims provided.
     *
     * @param claims  a map containing custom claims to include in the token. Can be null if no custom claims are
     *                required.
     * @param subject the subject of the token, typically representing the identity of the authenticated user.
     * @param roles   the roles assigned to the user.
     * @return a string representation of the generated JWT token.
     */
    public String createToken(final Map<String, Object> claims, final String subject, final Set<Role> roles) {
        final var now = Instant.now();
        final var expiresAt = now.plus(properties.jwt().ttl());

        final var builder = JwtClaimsSet.builder()
            .issuer(properties.jwt().issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(subject);
        if (claims != null) {
            claims.forEach(builder::claim);
        }
        if (roles != null && !roles.isEmpty()) {
            builder.claim(Oauth2SuccessHandler.ROLES_CLAIM, roles.stream().map(Enum::name).collect(Collectors.toSet()));
        }
        final var header = JwsHeader.with(SignatureAlgorithm.RS256)
            .type(TOKEN_TYPE)
            .keyId(rsaKeyProvider.getCurrentKid())
            .build();
        final var jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build()));
        return jwt.getTokenValue();
    }

    public static UUID resolveUserId(final Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
