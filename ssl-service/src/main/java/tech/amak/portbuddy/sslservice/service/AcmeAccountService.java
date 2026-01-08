/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.config.AppProperties;

/**
 * Loads or creates ACME account key pair from configured location.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcmeAccountService {

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;

    /**
     * Loads the ACME account {@link KeyPair} from {@code app.acme.accountKeyPath}.
     * The key must be in PEM format (PKCS#1 or PKCS#8). If the file does not exist, an exception is thrown.
     *
     * @return account key pair
     */
    public KeyPair loadAccountKeyPair() {
        final var pathString = properties.acme().accountKeyPath();
        final Resource resource = resourceLoader.getResource(pathString);
        try (final var reader = new InputStreamReader(resource.getInputStream())) {
            try (final var pemParser = new PEMParser(reader)) {
                final var obj = pemParser.readObject();
                final var converter = new JcaPEMKeyConverter();
                if (obj instanceof PEMKeyPair pemKeyPair) {
                    return converter.getKeyPair(pemKeyPair);
                }
                // Try DER fallback (PKCS8 private + X509 public alongside)
                final var file = resource.getFile();
                final var dir = file.getParentFile();
                final var privateDer = file.toPath();
                final var keyName = file.getName().replace(".pem", "");
                final var publicDer = dir.toPath().resolve(keyName + ".pub");
                if (Files.exists(privateDer) && Files.exists(publicDer)) {
                    final var factory = KeyFactory.getInstance("RSA");
                    final var priv = factory.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(privateDer)));
                    final var pub = factory.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(publicDer)));
                    return new KeyPair(pub, priv);
                }
                throw new IllegalStateException("Unsupported ACME account key format: " + obj);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read ACME account key at " + pathString, e);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to parse ACME account key at " + pathString, e);
        }
    }
}
