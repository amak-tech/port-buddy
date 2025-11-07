package tech.amak.portbuddy.server.web;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    /**
     * Placeholder token exchange for CLI. In real app, use OAuth2 login and JWT issuing.
     */
    @PostMapping("/token-exchange")
    public Map<String, Object> tokenExchange(final @RequestBody Map<String, Object> payload) {
        final var apiToken = String.valueOf(payload.getOrDefault("apiToken", ""));
        final var jwt = "jwt-" + Integer.toHexString(apiToken.hashCode());
        return Map.of("accessToken", jwt, "tokenType", "Bearer");
    }
}
