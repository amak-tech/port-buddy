/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.auth.RegisterRequest;
import tech.amak.portbuddy.common.dto.auth.RegisterResponse;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeRequest;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeResponse;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.service.ApiTokenService;
import tech.amak.portbuddy.server.user.UserProvisioningService;
import tech.amak.portbuddy.server.web.dto.LoginRequest;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final ApiTokenService apiTokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProvisioningService userProvisioningService;

    /**
     * Exchanges a valid API token for a short-lived JWT suitable for authenticating API and WebSocket calls.
     */
    @PostMapping("/token-exchange")
    public TokenExchangeResponse tokenExchange(final @RequestBody TokenExchangeRequest payload) {
        final var apiToken = payload == null ? "" : String.valueOf(payload.getApiToken()).trim();
        final var validatedOpt = apiTokenService.validateAndGetApiKey(apiToken);
        if (validatedOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API token");
        }
        final var validated = validatedOpt.get();
        final var userId = validated.userId();
        final var claims = new HashMap<String, Object>();
        claims.put("typ", "cli");
        claims.put("akid", validated.apiKeyId());
        final var jwt = jwtService.createToken(claims, userId);
        return new TokenExchangeResponse(jwt, "Bearer");
    }

    /**
     * Registers a new local user and returns an API key.
     */
    @PostMapping("/register")
    public RegisterResponse register(final @RequestBody RegisterRequest payload) {
        if (payload == null || payload.getEmail() == null || payload.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        try {
            final var provisioned = userProvisioningService.createLocalUser(
                payload.getEmail(),
                payload.getName(),
                payload.getPassword()
            );
            final var createdToken = apiTokenService.createToken(provisioned.userId(), "cli-init");
            return new RegisterResponse(createdToken.token());
        } catch (final IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
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
        claims.put("aid", user.getAccount().getId().toString());
        claims.put("uid", user.getId().toString());

        final var jwt = jwtService.createToken(claims, user.getId().toString());
        return new TokenExchangeResponse(jwt, "Bearer");
    }
}
