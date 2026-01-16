/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param accountId the account ID (UUID)
     * @param userId    the user ID (UUID)
     * @param label     optional label for the token
     * @return token id and raw token (displayed once)
     */
    @Transactional
    public CreatedToken createToken(final UUID accountId, final UUID userId, final String label) {
        final var rawToken = generateRawToken();
        final var tokenHash = sha256(rawToken);

        final var apiKey = new ApiKeyEntity();
        apiKey.setId(UUID.randomUUID());
        apiKey.setAccountId(accountId);
        apiKey.setUserId(userId);
        apiKey.setLabel(label == null || label.isBlank() ? "cli" : label);
        apiKey.setTokenHash(tokenHash);
        apiKey.setRevoked(false);

        final var saved = apiKeyRepository.save(apiKey);
        log.info("Created API token id={} for userId={} accountId={}", saved.getId(), userId, accountId);
        return new CreatedToken(saved.getId().toString(), rawToken);
    }

    /**
     * Lists tokens for the account without exposing secrets.
     */
    @Transactional(readOnly = true)
    public List<TokenView> listTokens(final UUID accountId) {
        return apiKeyRepository.findAllByAccountId(accountId).stream()
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
    public boolean revoke(final UUID accountId, final String tokenId) {
        final var tid = UUID.fromString(tokenId);
        return apiKeyRepository.findByIdAndAccountId(tid, accountId)
            .map(apiKey -> {
                apiKey.setRevoked(true);
                apiKey.setRevokedAt(OffsetDateTime.now());
                apiKeyRepository.save(apiKey);
                log.info("Revoked API token id={} for accountId={}", tid, accountId);
                return true;
            })
            .orElseGet(() -> {
                log.warn("Token {} not found for account {}", tokenId, accountId);
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

    /**
     * Validates raw token and returns user id, account id and api key id if valid.
     */
    @Transactional
    public Optional<ValidatedApiKey> validateAndGetApiKey(final String rawToken) {
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
        return Optional.of(new ValidatedApiKey(
            entity.getUserId().toString(),
            entity.getAccountId().toString(),
            entity.getId().toString()));
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

    public record ValidatedApiKey(String userId, String accountId, String apiKeyId) {
    }

    private static Instant toInstant(final OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }
}
