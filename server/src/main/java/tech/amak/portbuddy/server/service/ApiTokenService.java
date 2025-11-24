/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.db.entity.ApiKeyEntity;
import tech.amak.portbuddy.server.db.repo.ApiKeyRepository;

/**
 * API token service backed by the database. Stores only token hashes.
 */
@Service
@Slf4j
public class ApiTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final ApiKeyRepository apiKeyRepository;

    public ApiTokenService(final ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Creates a new API token for the specified user and stores its hash in the DB.
     *
     * @param userId the user ID (UUID string)
     * @param label  optional label for the token
     * @return token id and raw token (displayed once)
     */
    @Transactional
    public CreatedToken createToken(final String userId, final String label) {
        final var rawToken = generateRawToken();
        final var tokenHash = sha256(rawToken);

        final var apiKey = new ApiKeyEntity();
        apiKey.setId(UUID.randomUUID());
        apiKey.setUserId(UUID.fromString(userId));
        apiKey.setLabel(label == null || label.isBlank() ? "cli" : label);
        apiKey.setTokenHash(tokenHash);
        apiKey.setRevoked(false);

        final var saved = apiKeyRepository.save(apiKey);
        log.info("Created API token id={} for userId={}", saved.getId(), userId);
        return new CreatedToken(saved.getId().toString(), rawToken);
    }

    /**
     * Lists tokens for the user without exposing secrets.
     */
    @Transactional(readOnly = true)
    public List<TokenView> listTokens(final String userId) {
        final var uid = UUID.fromString(userId);
        return apiKeyRepository.findAllByUserId(uid).stream()
            .map(apiKey -> new TokenView(
                apiKey.getId().toString(),
                apiKey.getLabel(),
                toInstant(apiKey.getCreatedAt()),
                apiKey.isRevoked(),
                toInstant(apiKey.getLastUsedAt())))
            .toList();
    }

    /**
     * Marks a token as revoked.
     */
    @Transactional
    public boolean revoke(final String userId, final String tokenId) {
        final var uid = UUID.fromString(userId);
        final var tid = UUID.fromString(tokenId);
        return apiKeyRepository.findByIdAndUserId(tid, uid)
            .map(apiKey -> {
                apiKey.setRevoked(true);
                apiKey.setRevokedAt(OffsetDateTime.now());
                apiKeyRepository.save(apiKey);
                log.info("Revoked API token id={} for userId={}", tid, uid);
                return true;
            })
            .orElseGet(() -> {
                log.warn("Token {} not found for user {}", tokenId, userId);
                return false;
            });
    }

    /**
     * Validates raw token and returns user id if valid.
     */
    @Transactional
    public Optional<String> validateAndGetUserId(final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        final var hash = sha256(rawToken);
        final var entityOpt = apiKeyRepository.findByTokenHashAndRevokedFalse(hash);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }
        final var entity = entityOpt.get();
        entity.setLastUsedAt(OffsetDateTime.now());
        apiKeyRepository.save(entity);
        return Optional.of(entity.getUserId().toString());
    }

    private String generateRawToken() {
        final var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(final String val) {
        try {
            final var md = MessageDigest.getInstance("SHA-256");
            final var bytes = md.digest(val.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (final Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record CreatedToken(String id, String token) {
    }

    public record TokenView(String id, String label, Instant createdAt, boolean revoked, Instant lastUsedAt) {
    }

    private static Instant toInstant(final OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
