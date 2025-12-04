/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.PasswordResetTokenEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.PasswordResetTokenRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.EmailService;
import tech.amak.portbuddy.server.service.user.PasswordResetService;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppProperties properties;
    @Mock private AppProperties.Gateway gateway;

    @InjectMocks private PasswordResetService service;

    @Test
    void requestReset_shouldGenerateTokenAndSendEmail_whenUserExists() {
        final var email = "test@example.com";
        final var user = new UserEntity();
        user.setEmail(email);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(properties.gateway()).thenReturn(gateway);
        when(gateway.url()).thenReturn("http://localhost");

        service.requestReset(email);

        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(any(PasswordResetTokenEntity.class));
        verify(emailService).sendTemplate(eq(email), anyString(), eq("email/password-reset"), anyMap());
    }

    @Test
    void requestReset_shouldDoNothing_whenUserDoesNotExist() {
        final var email = "unknown@example.com";
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        service.requestReset(email);

        verify(tokenRepository, org.mockito.Mockito.never()).save(any());
        verify(emailService, org.mockito.Mockito.never()).sendTemplate(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void validateToken_shouldReturnTrue_whenTokenIsValid() {
        final var token = "valid-token";
        final var entity = new PasswordResetTokenEntity();
        entity.setToken(token);
        entity.setExpiryDate(OffsetDateTime.now().plusHours(1));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(entity));

        assertTrue(service.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsExpired() {
        final var token = "expired-token";
        final var entity = new PasswordResetTokenEntity();
        entity.setToken(token);
        entity.setExpiryDate(OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(entity));

        assertFalse(service.validateToken(token));
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        final var token = "valid-token";
        final var newPass = "new-password";
        final var entity = new PasswordResetTokenEntity();
        entity.setToken(token);
        entity.setExpiryDate(OffsetDateTime.now().plusHours(1));
        final var user = new UserEntity();
        user.setEmail("user@example.com");
        entity.setUser(user);

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(entity));
        when(passwordEncoder.encode(newPass)).thenReturn("encoded-password");
        when(properties.gateway()).thenReturn(gateway);
        when(gateway.url()).thenReturn("http://localhost");

        service.resetPassword(token, newPass);

        verify(userRepository).save(user);
        verify(tokenRepository).delete(entity);
        verify(emailService)
            .sendTemplate(eq(user.getEmail()), anyString(), eq("email/password-reset-success"), anyMap());
    }

    @Test
    void resetPassword_shouldThrow_whenTokenIsExpired() {
        final var token = "expired-token";
        final var entity = new PasswordResetTokenEntity();
        entity.setToken(token);
        entity.setExpiryDate(OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(token, "new-pass"));
    }
}
