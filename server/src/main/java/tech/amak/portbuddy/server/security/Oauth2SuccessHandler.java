/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.service.user.MissingEmailException;
import tech.amak.portbuddy.server.service.user.UserProvisioningService;

@Component
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    public static final String EMAIL_CLAIM = "email";
    public static final String NAME_CLAIM = "name";
    public static final String PICTURE_CLAIM = "picture";
    public static final String AVATAR_URL_CLAIM = "avatar_url";
    public static final String FIRST_NAME_CLAIM = "given_name";
    public static final String LAST_NAME_CLAIM = "family_name";
    public static final String ACCOUNT_ID_CLAIM = "aid";
    public static final String ACCOUNT_NAME_CLAIM = "aname";
    public static final String USER_ID_CLAIM = "uid";
    public static final String SUBJECT_CLAIM = "sub";
    public static final String ID_CLAIM = "id";
    public static final String ROLES_CLAIM = "roles";
    private static final String UNKNOWN = "unknown";

    private final JwtService jwtService;
    private final AppProperties properties;
    private final UserProvisioningService userProvisioningService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient;

    public Oauth2SuccessHandler(final JwtService jwtService,
                                final AppProperties properties,
                                final UserProvisioningService userProvisioningService,
                                final OAuth2AuthorizedClientService authorizedClientService,
                                final RestClient.Builder restClientBuilder) {
        this.jwtService = jwtService;
        this.properties = properties;
        this.userProvisioningService = userProvisioningService;
        this.authorizedClientService = authorizedClientService;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication)
        throws IOException {
        final var provider = resolveProvider(authentication);
        var externalId = UNKNOWN;
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
            picture = (String) oidc.getClaims().getOrDefault(PICTURE_CLAIM, null);
            firstName = (String) oidc.getClaims().getOrDefault(FIRST_NAME_CLAIM, null);
            lastName = (String) oidc.getClaims().getOrDefault(LAST_NAME_CLAIM, null);
        } else if (principal instanceof OAuth2User oauth2) {
            final var attrs = oauth2.getAttributes();
            externalId = String.valueOf(attrs.getOrDefault(SUBJECT_CLAIM, attrs.getOrDefault(ID_CLAIM, UNKNOWN)));
            email = asNullableString(attrs, EMAIL_CLAIM);
            name = asNullableString(attrs, NAME_CLAIM);
            picture = asNullableString(attrs, PICTURE_CLAIM);
            if (picture == null) {
                picture = asNullableString(attrs, AVATAR_URL_CLAIM);
            }
            if (attrs.containsKey(FIRST_NAME_CLAIM) || attrs.containsKey(LAST_NAME_CLAIM)) {
                firstName = asNullableString(attrs, FIRST_NAME_CLAIM);
                lastName = asNullableString(attrs, LAST_NAME_CLAIM);
            }

            if (email == null && "github".equals(provider)) {
                email = fetchGithubEmail(authentication);
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
        final var provisioned = provisionOrRedirectOnMissingEmail(
            response, provider, externalId, email, firstName, lastName, picture);
        if (provisioned == null) {
            // Redirect already sent (e.g., missing email). Stop further processing.
            return;
        }

        final var claims = new HashMap<String, Object>();
        if (email != null) {
            claims.put(EMAIL_CLAIM, email);
        }
        if (name != null) {
            claims.put(NAME_CLAIM, name);
        }
        if (picture != null) {
            claims.put(PICTURE_CLAIM, picture);
        }
        claims.put(ACCOUNT_ID_CLAIM, provisioned.accountId().toString());
        claims.put(ACCOUNT_NAME_CLAIM, provisioned.accountName());
        claims.put(USER_ID_CLAIM, provisioned.userId().toString());

        final var token = jwtService.createToken(claims, provisioned.userId().toString(), provisioned.roles());
        final var redirectUrl = properties.gateway().url() + "/auth/callback?token=" + URLEncoder.encode(token, UTF_8);
        response.sendRedirect(redirectUrl);
    }

    private static String resolveProvider(final Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getAuthorizedClientRegistrationId();
        }
        return "unknown";
    }

    private String fetchGithubEmail(final Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return null;
        }
        final var client = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName());
        if (client == null || client.getAccessToken() == null) {
            return null;
        }

        try {
            final var emails = restClient.get()
                .uri("https://api.github.com/user/emails")
                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });

            if (emails == null || emails.isEmpty()) {
                return null;
            }

            // Prefer primary email
            return emails.stream()
                .filter(map ->
                    Boolean.TRUE.equals(map.get("primary")) && Boolean.TRUE.equals(map.get("verified")))
                .map(map -> (String) map.get("email"))
                .findFirst()
                .orElseGet(() -> emails.stream()
                    .filter(map -> Boolean.TRUE.equals(map.get("verified")))
                    .map(map -> (String) map.get("email"))
                    .findFirst()
                    .orElse(null));
        } catch (final Exception e) {
            return null;
        }
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
        } catch (final MissingEmailException ex) {
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
