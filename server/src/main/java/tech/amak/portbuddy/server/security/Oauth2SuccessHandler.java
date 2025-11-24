/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.user.UserProvisioningService;
import tech.amak.portbuddy.server.user.MissingEmailException;

@Component
@RequiredArgsConstructor
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AppProperties properties;
    private final UserProvisioningService userProvisioningService;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication)
        throws IOException {
        var provider = resolveProvider(authentication);
        var externalId = "unknown";
        var email = (String) null;
        var name = (String) null;
        var picture = (String) null;
        var firstName = (String) null;
        var lastName = (String) null;

        final var principal = authentication.getPrincipal();
        if (principal instanceof DefaultOidcUser oidc) {
            externalId = oidc.getSubject();
            email = oidc.getEmail();
            name = oidc.getFullName();
            picture = (String) oidc.getClaims().getOrDefault("picture", null);
            firstName = (String) oidc.getClaims().getOrDefault("given_name", null);
            lastName = (String) oidc.getClaims().getOrDefault("family_name", null);
        } else if (principal instanceof OAuth2User oauth2) {
            final var attrs = oauth2.getAttributes();
            externalId = String.valueOf(attrs.getOrDefault("sub", attrs.getOrDefault("id", "unknown")));
            email = asNullableString(attrs, "email");
            name = asNullableString(attrs, "name");
            picture = asNullableString(attrs, "picture");
            if (attrs.containsKey("given_name") || attrs.containsKey("family_name")) {
                firstName = asNullableString(attrs, "given_name");
                lastName = asNullableString(attrs, "family_name");
            }
        }

        if ((firstName == null || lastName == null) && name != null && !name.isBlank()) {
            final var parts = name.trim().split(" ", 2);
            firstName = firstName == null ? parts[0] : firstName;
            if (parts.length > 1) {
                lastName = lastName == null ? parts[1] : lastName;
            }
        }

        // Ensure user + account exist or updated
        final var provisioned = provisionOrRedirectOnMissingEmail(response, provider, externalId, email, firstName, lastName, picture);
        if (provisioned == null) {
            // Redirect already sent (e.g., missing email). Stop further processing.
            return;
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
        claims.put("aid", provisioned.accountId().toString());
        claims.put("uid", provisioned.userId().toString());

        final var token = jwtService.createToken(claims, provisioned.userId().toString());
        final var redirectUrl = properties.gateway().url() + "/auth/callback?token=" + URLEncoder.encode(token, UTF_8);
        response.sendRedirect(redirectUrl);
    }

    private static String resolveProvider(final Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getAuthorizedClientRegistrationId();
        }
        return "unknown";
    }

    private static String asNullableString(final Map<String, Object> attrs, final String key) {
        final var value = attrs.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private UserProvisioningService.ProvisionedUser provisionOrRedirectOnMissingEmail(
        final HttpServletResponse response,
        final String provider,
        final String externalId,
        final String email,
        final String firstName,
        final String lastName,
        final String picture) throws IOException {
        try {
            return userProvisioningService.provision(provider, externalId, email, firstName, lastName, picture);
        } catch (MissingEmailException ex) {
            final var url = properties.gateway().url()
                + "/auth/callback?error="
                + URLEncoder.encode("missing_email", UTF_8)
                + "&message="
                + URLEncoder.encode("Email is required to sign in", UTF_8);
            response.sendRedirect(url);
            return null;
        }
    }
}
