/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.ApiTokenService;

@RestController
@RequestMapping(path = "/api/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TokensController {

    private final ApiTokenService apiTokenService;
    private final UserRepository userRepository;

    /**
     * Retrieves a list of API tokens belonging to the authenticated user.
     *
     * @param principal the authenticated user's principal object, used to extract the user ID
     * @return a list of {@code ApiTokenService.TokenView} objects representing the user's API tokens
     * @throws ResponseStatusException if the authenticated user cannot be found in the database
     */
    @GetMapping
    public List<ApiTokenService.TokenView> list(@AuthenticationPrincipal final Jwt principal) {
        final var userId = resolveUserId(principal);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return apiTokenService.listTokens(user.getAccount().getId());
    }

    /**
     * Creates a new API token for the authenticated user.
     *
     * @param principal the authenticated user's principal object, used to extract the user ID
     * @param req       the request payload containing the label for the API token; can be null
     * @return a {@code CreateTokenResponse} object containing the generated token ID and the token itself
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CreateTokenResponse create(@AuthenticationPrincipal final Jwt principal,
                                      @RequestBody final CreateTokenRequest req) {
        final var userId = resolveUserId(principal);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        final var created = apiTokenService.createToken(
            user.getAccount().getId(),
            userId,
            req == null ? null : req.getLabel());
        return new CreateTokenResponse(created.id(), created.token());
    }

    /**
     * Revokes an API token associated with the authenticated user's account.
     *
     * @param principal the authenticated user's principal object, used to extract the user ID
     * @param id        the ID of the API token to be revoked
     * @throws ResponseStatusException if the authenticated user cannot be found in the database
     */
    @DeleteMapping("/{id}")
    public void revoke(@AuthenticationPrincipal final Jwt principal, @PathVariable("id") final String id) {
        final var userId = resolveUserId(principal);
        final var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        apiTokenService.revoke(user.getAccount().getId(), id);
    }

    @Data
    public static class CreateTokenRequest {
        private String label;
    }

    public record CreateTokenResponse(String id, String token) {
    }
}
