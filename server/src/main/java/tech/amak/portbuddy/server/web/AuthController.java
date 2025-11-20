package tech.amak.portbuddy.server.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.service.ApiTokenService;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final ApiTokenService apiTokenService;
    private final JwtService jwtService;

    /**
     * Exchanges a valid API token for a short-lived JWT suitable for authenticating API and WebSocket calls.
     * Body: { "apiToken": "..." }
     * Response: { "accessToken": "<jwt>", "tokenType": "Bearer" }
     */
    @PostMapping("/token-exchange")
    public Map<String, Object> tokenExchange(final @RequestBody Map<String, Object> payload) {
        final var apiToken = String.valueOf(payload.getOrDefault("apiToken", "")).trim();
        final var userIdOpt = apiTokenService.validateAndGetUserId(apiToken);
        if (userIdOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API token");
        }
        final var userId = userIdOpt.get();
        final var jwt = jwtService.createToken(Map.of("typ", "cli"), userId);
        return Map.of("accessToken", jwt, "tokenType", "Bearer");
    }
}
