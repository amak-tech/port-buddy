/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.util.KeyPairUtils;
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

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;

    /**
     * Loads the ACME account {@link KeyPair} from {@code app.acme.accountKeyPath}.
     * The key must be in PEM format (PKCS#1 or PKCS#8). If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return account key pair
     */
    public KeyPair loadAccountKeyPair() {
        final var pathString = properties.acme().accountKeyPath();
        final var resource = resourceLoader.getResource(pathString);

        if (resource.exists()) {
            try (final var reader = new InputStreamReader(resource.getInputStream())) {
                return KeyPairUtils.readKeyPair(reader);
            } catch (final IOException e) {
                log.error("Failed to load ACME account key pair from {}", pathString, e);
                throw new IllegalStateException("Failed to load ACME account key pair", e);
            }
        } else {
            final var keyPair = KeyPairUtils.createKeyPair();
            try {
                final var file = resource.getFile();
                final var parentFile = file.getParentFile();
                if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentFile);
                }
                try (final var writer = Files.newBufferedWriter(file.toPath())) {
                    KeyPairUtils.writeKeyPair(keyPair, writer);
                }
            } catch (final IOException e) {
                log.error("Failed to save ACME account key pair to {}", pathString, e);
                throw new IllegalStateException("Failed to save ACME account key pair", e);
            }
            return keyPair;
        }
    }
}
