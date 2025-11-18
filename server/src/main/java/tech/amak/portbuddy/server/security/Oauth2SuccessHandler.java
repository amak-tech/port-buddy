package tech.amak.portbuddy.server.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Component
@RequiredArgsConstructor
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AppProperties properties;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication)
        throws IOException {
        String userId = "unknown";
        String email = null;
        String name = null;
        String picture = null;

        final var principal = authentication.getPrincipal();
        if (principal instanceof DefaultOidcUser oidc) {
            userId = oidc.getSubject();
            email = oidc.getEmail();
            name = oidc.getFullName();
            picture = (String) oidc.getClaims().getOrDefault("picture", null);
        } else if (principal instanceof OAuth2User oauth2) {
            final var attrs = oauth2.getAttributes();
            userId = String.valueOf(attrs.getOrDefault("sub", attrs.getOrDefault("id", "unknown")));
            email = String.valueOf(attrs.getOrDefault("email", ""));
            name = String.valueOf(attrs.getOrDefault("name", ""));
            picture = String.valueOf(attrs.getOrDefault("picture", ""));
        }

        final var claims = new HashMap<String, Object>();
        if (email != null) {
            claims.put("email", email);
        }
        if (name != null) {
            claims.put("name", name);
        }
        if (picture != null) {
            claims.put("picture", picture);
        }

        final var token = jwtService.createToken(claims, userId);
        final var redirectUrl = properties.gateway().url() + "/auth/callback?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
