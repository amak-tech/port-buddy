/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import java.util.List;

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

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.service.ApiTokenService;

@RestController
@RequestMapping(path = "/api/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TokensController {

    private final ApiTokenService apiTokenService;

    @GetMapping
    public List<ApiTokenService.TokenView> list(@AuthenticationPrincipal final Jwt principal) {
        final var userId = resolveUserId(principal);
        return apiTokenService.listTokens(userId);
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
        final var created = apiTokenService.createToken(userId, req == null ? null : req.getLabel());
        return new CreateTokenResponse(created.id(), created.token());
    }

    @DeleteMapping("/{id}")
    public void revoke(@AuthenticationPrincipal final Jwt principal, @PathVariable("id") final String id) {
        final var userId = resolveUserId(principal);
        apiTokenService.revoke(userId, id);
    }

    @Data
    public static class CreateTokenRequest {
        private String label;
    }

    public record CreateTokenResponse(String id, String token) {
    }
}
