/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service.user;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.PasswordResetTokenEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.PasswordResetTokenRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    /**
     * Initiates the password reset process for the given email.
     *
     * @param email the user's email address
     */
    @Transactional
    public void requestReset(final String email) {
        final var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            // Do not reveal that user does not exist
            return;
        }

        final var user = userOpt.get();

        final var resetLink = generateResetPasswordLink(user, Duration.ofHours(1));

        final var model = new HashMap<String, Object>();
        model.put("subject", "Reset Your Password");
        model.put("resetLink", resetLink);
        model.put("webAppUrl", properties.gateway().url());

        final var firstName = user.getFirstName();
        model.put("greeting", firstName == null ? "Hello!" : "Hello " + firstName + ",");

        emailService.sendTemplate(user.getEmail(), "Reset Your Password", "email/password-reset", model);
        log.info("Password reset email sent to user: {}", user.getId());
    }

    /**
     * Generates a reset password link for the given user and token time-to-live (TTL).
     * This method invalidates any existing reset tokens for the user, creates a new
     * token, saves it in the database, and returns the corresponding reset link.
     *
     * @param user the user entity for whom the reset password link is being generated
     * @param ttl the duration for which the reset token is valid
     * @return the complete reset password link containing the generated token
     */
    @Transactional
    public String generateResetPasswordLink(final UserEntity user, final Duration ttl) {

        // Invalidate existing tokens
        tokenRepository.deleteByUser(user);

        final var token = UUID.randomUUID().toString();
        final var entity = new PasswordResetTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setToken(token);
        entity.setUser(user);
        // Token TTL
        entity.setExpiryDate(OffsetDateTime.now().plus(ttl));
        tokenRepository.save(entity);

        return properties.gateway().url() + "/reset-password?token=" + token;
    }

    /**
     * Validates if the reset token exists and is not expired.
     *
     * @param token the reset token
     * @return true if valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateToken(final String token) {
        final var tokenEntityOpt = tokenRepository.findByToken(token);
        if (tokenEntityOpt.isEmpty()) {
            return false;
        }
        final var tokenEntity = tokenEntityOpt.get();
        return tokenEntity.getExpiryDate().isAfter(OffsetDateTime.now());
    }

    /**
     * Resets the user's password using the valid token.
     *
     * @param token       the reset token
     * @param newPassword the new password
     */
    @Transactional
    public void resetPassword(final String token, final String newPassword) {
        final var tokenEntity = tokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (tokenEntity.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        final var user = tokenEntity.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(tokenEntity);

        // Send confirmation email
        final var model = new HashMap<String, Object>();
        model.put("subject", "Password Changed Successfully");
        model.put("webAppUrl", properties.gateway().url() + "/app");
        final var firstName = user.getFirstName();
        model.put("greeting", firstName == null ? "Hello!" : "Hello " + firstName + ",");

        emailService.sendTemplate(user.getEmail(), "Password Changed Successfully",
            "email/password-reset-success", model);
        log.info("Password reset successfully for user: {}", user.getId());
    }
}
