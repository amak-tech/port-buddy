/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.auth.RegisterRequest;
import tech.amak.portbuddy.common.dto.auth.RegisterResponse;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeRequest;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeResponse;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.service.ApiTokenService;
import tech.amak.portbuddy.server.service.user.PasswordResetService;
import tech.amak.portbuddy.server.service.user.UserProvisioningService;
import tech.amak.portbuddy.server.web.dto.LoginRequest;
import tech.amak.portbuddy.server.web.dto.PasswordResetConfirm;
import tech.amak.portbuddy.server.web.dto.PasswordResetRequest;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final ApiTokenService apiTokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProvisioningService userProvisioningService;
    private final PasswordResetService passwordResetService;
    private final AppProperties properties;
    private final UserAccountRepository userAccountRepository;

    /**
     * Exchanges a valid API token for a short-lived JWT suitable for authenticating API and WebSocket calls.
     */
    @PostMapping("/token-exchange")
    public TokenExchangeResponse tokenExchange(final @RequestBody TokenExchangeRequest payload) {
        final var apiToken = payload == null ? "" : String.valueOf(payload.getApiToken()).trim();
        final var cliClientVersion = payload == null ? null : payload.getCliClientVersion();
        if (cliClientVersion == null || cliClientVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UPGRADE_REQUIRED,
                "CLI client version is missing or not supported. Please upgrade your port-buddy CLI.");
        }
        if (!isCliVersionSupported(cliClientVersion.trim(), properties.cli().minVersion())) {
            throw new ResponseStatusException(HttpStatus.UPGRADE_REQUIRED,
                "Your port-buddy CLI is outdated. Please upgrade to the latest version.");
        }
        final var validatedOpt = apiTokenService.validateAndGetApiKey(apiToken);
        if (validatedOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API token");
        }
        final var validated = validatedOpt.get();
        final var userId = validated.userId();
        final var claims = new HashMap<String, Object>();
        claims.put("typ", "cli");
        claims.put("akid", validated.apiKeyId());
        claims.put("aid", validated.accountId());
        final var jwt = jwtService.createToken(claims, userId);
        return new TokenExchangeResponse(jwt, "Bearer");
    }

    private boolean isCliVersionSupported(final String clientVersion, final String minimalVersion) {
        // allow dev builds
        final var cv = clientVersion.toLowerCase();
        if (cv.contains("dev")) {
            return true;
        }
        return compareVersions(clientVersion, minimalVersion) >= 0;
    }

    private int compareVersions(final String v1, final String v2) {
        final var a = v1.split("[.\\-]");
        final var b = v2.split("[.\\-]");
        final var len = Math.max(a.length, b.length);
        for (var i = 0; i < len; i++) {
            final var ai = i < a.length ? parseIntSafe(a[i]) : 0;
            final var bi = i < b.length ? parseIntSafe(b[i]) : 0;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    private int parseIntSafe(final String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (final Exception ignored) {
            return 0;
        }
    }

    /**
     * Registers a new local user and returns an API key.
     */
    @PostMapping("/register")
    public RegisterResponse register(final @RequestBody RegisterRequest payload) {
        if (payload == null || payload.getEmail() == null) {
            return new RegisterResponse(null, false, "Email is required", 400);
        }

        try {
            final var provisioned = userProvisioningService.createLocalUser(
                payload.getEmail(),
                payload.getName(),
                payload.getPassword()
            );
            final var createdToken = apiTokenService.createToken(
                provisioned.accountId(), provisioned.userId(), "prtb-client");
            return new RegisterResponse(createdToken.token(), true, "User registered successfully", 200);
        } catch (final IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return new RegisterResponse(null, false, e.getMessage(), 400);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return new RegisterResponse(null, false, "Internal Server Error: " + e.getMessage(), 500);
        }
    }

    /**
     * Authenticates a user with email and password.
     */
    @PostMapping("/login")
    public TokenExchangeResponse login(final @RequestBody LoginRequest payload) {
        if (payload == null || payload.email() == null || payload.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        final var user = userRepository.findByEmailIgnoreCase(payload.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(payload.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        final var claims = new HashMap<String, Object>();
        claims.put("email", user.getEmail());

        String name = user.getFirstName();
        if (user.getLastName() != null) {
            name = name + " " + user.getLastName();
        }
        claims.put("name", name.trim());

        if (user.getAvatarUrl() != null) {
            claims.put("picture", user.getAvatarUrl());
        }

        final var userAccount = userAccountRepository.findLatestUsedByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User has no accounts"));

        claims.put("aid", userAccount.getAccount().getId().toString());
        claims.put("uid", user.getId().toString());

        final var jwt = jwtService.createToken(claims, user.getId().toString(), userAccount.getRoles());
        return new TokenExchangeResponse(jwt, "Bearer");
    }

    /**
     * Requests a password reset email.
     */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(final @RequestBody PasswordResetRequest payload) {
        if (payload == null || payload.email() == null || payload.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        passwordResetService.requestReset(payload.email());
    }

    /**
     * Validates a password reset token.
     */
    @GetMapping("/password-reset/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validateResetToken(final @RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
        }
        if (!passwordResetService.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }
    }

    /**
     * Confirms password reset and sets a new password.
     */
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(final @RequestBody PasswordResetConfirm payload) {
        if (payload == null || payload.token() == null || payload.newPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token and new password are required");
        }
        try {
            passwordResetService.resetPassword(payload.token(), payload.newPassword());
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
