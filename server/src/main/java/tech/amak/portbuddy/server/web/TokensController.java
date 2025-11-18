package tech.amak.portbuddy.server.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public List<ApiTokenService.TokenView> list(@AuthenticationPrincipal final Object principal) {
        final var userId = extractUserId(principal);
        return apiTokenService.listTokens(userId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public CreateTokenResponse create(@AuthenticationPrincipal final Object principal, @RequestBody final CreateTokenRequest req) {
        final var userId = extractUserId(principal);
        final var created = apiTokenService.createToken(userId, req == null ? null : req.getLabel());
        return new CreateTokenResponse(created.id(), created.token());
    }

    @DeleteMapping("/{id}")
    public void revoke(@AuthenticationPrincipal final Object principal, @PathVariable("id") final String id) {
        final var userId = extractUserId(principal);
        apiTokenService.revoke(userId, id);
    }

    private String extractUserId(final Object principal) {
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        if (principal instanceof String s) {
            return s;
        } else if (principal instanceof DefaultOidcUser oidc) {
            return oidc.getSubject();
        } else if (principal instanceof OAuth2User oauth2User) {
            final var attrs = oauth2User.getAttributes();
            return String.valueOf(attrs.getOrDefault("sub", attrs.getOrDefault("id", "unknown")));
        }
        throw new RuntimeException("Unauthenticated");
    }

    @Data
    public static class CreateTokenRequest {
        private String label;
    }

    public record CreateTokenResponse(String id, String token) {
    }
}
