package tech.amak.portbuddy.server.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;

/**
 * Loads RSA keys from configuration and exposes a JWKSource for signing and a public JWKSet for discovery.
 */
@Component
@Slf4j
public class RsaKeyProvider {

    private final ImmutableJWKSet<SecurityContext> jwkSource;
    @Getter
    private final JWKSet publicJwkSet;
    @Getter
    private final String currentKid;

    public RsaKeyProvider(final AppProperties properties) {
        final var jwt = Objects.requireNonNull(properties.jwt(), "app.jwt must be configured");
        final var rsa = Objects.requireNonNull(jwt.rsa(), "app.jwt.rsa must be configured");
        this.currentKid = Objects.requireNonNull(rsa.currentKeyId(), "app.jwt.rsa.currentKeyId must be set");
        final var keys = Objects.requireNonNull(rsa.keys(), "app.jwt.rsa.keys must be set");
        if (keys.isEmpty()) {
            throw new IllegalStateException("app.jwt.rsa.keys must contain at least one key");
        }

        final List<RSAKey> all = new ArrayList<>();
        final List<RSAKey> publicOnly = new ArrayList<>();
        for (final var key : keys) {
            final var id = Objects.requireNonNull(key.id(), "RSA key id must be set");
            try {
                final var pub = parsePublicKey(key.publicKeyPem().getContentAsString(StandardCharsets.UTF_8));
                final var builder = new RSAKey.Builder(pub).keyID(id);
                publicOnly.add(builder.build());

                final var privatePem = key.privateKeyPem().getContentAsString(StandardCharsets.UTF_8);
                if (!privatePem.isBlank()) {
                    final var rsaPrivateKey = parsePrivateKey(privatePem);
                    all.add(new RSAKey.Builder(pub).privateKey(rsaPrivateKey).keyID(id).build());
                } else {
                    // Public-only key; not usable for signing
                    all.add(builder.build());
                }
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to parse RSA key id=" + id + ": " + e.getMessage(), e);
            }
        }

        final var allJwks = new ArrayList<JWK>(all);
        final var publicJwks = new ArrayList<JWK>(publicOnly);
        this.jwkSource = new ImmutableJWKSet<>(new JWKSet(allJwks));
        this.publicJwkSet = new JWKSet(publicJwks);
        log.info("Loaded {} RSA keys; current kid={}", all.size(), this.currentKid);
    }

    public JWKSource<SecurityContext> jwkSource() {
        return jwkSource;
    }

    private static RSAPublicKey parsePublicKey(final String pem) throws Exception {
        final var content = stripPemHeaders(pem, "PUBLIC KEY");
        final var decoded = Base64.getMimeDecoder().decode(content);
        final var kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static RSAPrivateKey parsePrivateKey(final String pem) throws Exception {
        final var content = stripPemHeaders(pem, "PRIVATE KEY");
        final var decoded = Base64.getMimeDecoder().decode(content);
        final var kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private static String stripPemHeaders(final String pem, final String type) {
        if (pem == null) {
            throw new IllegalArgumentException("PEM is null");
        }
        return pem
            .replace("-----BEGIN " + type + "-----", "")
            .replace("-----END " + type + "-----", "")
            .replaceAll("\\s", "");
    }
}
