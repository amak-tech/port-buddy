package tech.amak.portbuddy.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple in-memory API token service. Stores only token hashes in memory.
 * For production, back this with persistent storage.
 */
@Service
@Slf4j
public class ApiTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    // tokenHash -> record
    private final Map<String, TokenRecord> tokensByHash = new ConcurrentHashMap<>();
    // userId -> list of records
    private final Map<String, List<TokenRecord>> tokensByUser = new ConcurrentHashMap<>();

    /**
     * Creates a new API token for the specified user and stores it in memory.
     *
     * @param userId the unique identifier of the user for whom the token is being created
     * @param label  a user-defined label for the token; if null or blank, a default value of "cli" will be used
     * @return a {@code CreatedToken} object containing the generated token and its corresponding ID
     */
    public CreatedToken createToken(final String userId, final String label) {
        final var rawToken = generateRawToken();
        final var tokenHash = sha256(rawToken);
        final var rec = new TokenRecord();
        rec.setId(UUID.randomUUID().toString());
        rec.setUserId(userId);
        rec.setLabel(label == null || label.isBlank() ? "cli" : label);
        rec.setTokenHash(tokenHash);
        rec.setCreatedAt(Instant.now());
        rec.setRevoked(false);
        tokensByHash.put(tokenHash, rec);
        tokensByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(rec);
        log.info("Created API token id={} for userId={}", rec.getId(), userId);
        return new CreatedToken(rec.getId(), rawToken);
    }

    /**
     * Retrieves a list of tokens associated with the specified user.
     * Each token is represented as a {@code TokenView} object containing relevant details.
     *
     * @param userId the unique identifier of the user whose tokens should be listed
     * @return a list of {@code TokenView} objects representing the user's tokens;
     *     if the user has no tokens, an empty list is returned
     */
    public List<TokenView> listTokens(final String userId) {
        final var list = tokensByUser.getOrDefault(userId, List.of());
        final var out = new ArrayList<TokenView>(list.size());
        for (var r : list) {
            out.add(new TokenView(r.getId(), r.getLabel(), r.getCreatedAt(), r.isRevoked(), r.getLastUsedAt()));
        }
        return out;
    }

    /**
     * Revokes a specific API token associated with a user, effectively rendering it invalid.
     * The token is removed from the internal storage upon successful revocation.
     *
     * @param userId  the unique identifier of the user who owns the token
     * @param tokenId the unique identifier of the token to be revoked
     * @return {@code true} if the token was successfully revoked, {@code false} if the token
     *     was not found or could not be revoked
     */
    public boolean revoke(final String userId, final String tokenId) {
        final var list = tokensByUser.getOrDefault(userId, List.of());
        for (var r : list) {
            if (r.getId().equals(tokenId)) {
                r.setRevoked(true);
                tokensByHash.remove(r.getTokenHash());
                return true;
            }
        }
        return false;
    }

    /**
     * Validates the given raw token and retrieves the associated user ID if the token is valid.
     * A token is considered valid if it exists in the token storage, has not been revoked,
     * and is not expired. Additionally, the method updates the last used timestamp of the token.
     *
     * @param rawToken the raw API token to validate; can be null or blank
     * @return an {@code Optional} containing the user ID if the token is valid;
     *     otherwise, an empty {@code Optional}
     */
    public Optional<String> validateAndGetUserId(final String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        final var hash = sha256(rawToken);
        final var token = tokensByHash.get(hash);
        if (token == null || token.isRevoked()) {
            return Optional.empty();
        }
        token.setLastUsedAt(Instant.now());
        return Optional.of(token.getUserId());
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
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Data
    public static class TokenRecord {
        private String id;
        private String userId;
        private String label;
        private String tokenHash;
        private Instant createdAt;
        private Instant lastUsedAt;
        private boolean revoked;
    }

    public record CreatedToken(String id, String token) {
    }

    public record TokenView(String id, String label, Instant createdAt, boolean revoked, Instant lastUsedAt) {
    }
}
