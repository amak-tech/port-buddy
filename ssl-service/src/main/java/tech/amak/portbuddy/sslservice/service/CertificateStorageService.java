/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.config.AppProperties;

/**
 * Handles certificate and key file storage in PEM format.
 */
@Service
@RequiredArgsConstructor
public class CertificateStorageService {

    private final AppProperties properties;

    /**
     * Generates a new RSA key pair.
     *
     * @return generated key pair
     */
    public KeyPair generateRsaKeyPair() {
        try {
            final var kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, SecureRandom.getInstanceStrong());
            return kpg.generateKeyPair();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }

    /**
     * Writes a private key to PEM file.
     *
     * @param domain  domain name (used for file names)
     * @param keyPair key pair
     * @return path to written private key file
     */
    public Path writePrivateKeyPem(final String domain, final KeyPair keyPair) {
        final var baseDir = resolveBaseDir();
        final var file = baseDir.resolve(safe(domain) + ".key.pem");
        writePem(file, keyPair.getPrivate());
        return file;
    }

    /**
     * Writes certificate chain to PEM file.
     *
     * @param domain   domain
     * @param chainPem full chain in PEM string
     * @return path to chain file
     */
    public Path writeChainPem(final String domain, final String chainPem) {
        final var baseDir = resolveBaseDir();
        final var file = baseDir.resolve(safe(domain) + ".chain.pem");
        writeString(file, chainPem);
        return file;
    }

    /**
     * Writes leaf certificate to PEM file.
     *
     * @param domain  domain
     * @param certPem certificate PEM
     * @return path to cert file
     */
    public Path writeCertPem(final String domain, final String certPem) {
        final var baseDir = resolveBaseDir();
        final var file = baseDir.resolve(safe(domain) + ".cert.pem");
        writeString(file, certPem);
        return file;
    }

    private Path resolveBaseDir() {
        final var resource = properties.storage().certificatesDir();
        // Expecting formats like "file:/abs/path" or just a directory. Normalize to Path.
        final Path dir;
        if (resource.startsWith("file:")) {
            dir = Path.of(resource.substring("file:".length()));
        } else {
            dir = Path.of(resource);
        }
        try {
            Files.createDirectories(dir);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot create certificates directory: " + dir, e);
        }
        return dir;
    }

    private void writePem(final Path file, final Object pemObject) {
        try {
            try (var os = Files.newOutputStream(file);
                 var writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 var pemWriter = new JcaPEMWriter(writer)) {
                pemWriter.writeObject(pemObject);
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to write PEM file: " + file, e);
        }
    }

    private void writeString(final Path file, final String content) {
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to write file: " + file, e);
        }
    }

    private String safe(final String domain) {
        return domain.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
    }
}
