/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.service.user.MissingEmailException;
import tech.amak.portbuddy.server.service.user.UserProvisioningService;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class Oauth2SuccessHandlerTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private AppProperties properties;
    @Mock
    private UserProvisioningService userProvisioningService;
    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;

    private Oauth2SuccessHandler handler;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        handler = new Oauth2SuccessHandler(jwtService, properties, userProvisioningService, authorizedClientService, restClientBuilder);

        // AppProperties config
        AppProperties.Gateway gateway = mock(AppProperties.Gateway.class);
        when(properties.gateway()).thenReturn(gateway);
        when(gateway.url()).thenReturn("http://localhost:8443");
    }

    @Test
    void onAuthenticationSuccess_WithEmail_ShouldProvisionAndRedirect() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        OAuth2User principal = new DefaultOAuth2User(List.of(), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, List.of(), "google");

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(userProvisioningService.provision(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new UserProvisioningService.ProvisionedUser(userId, accountId, "Test Account", Collections.emptySet()));
        when(jwtService.createToken(any(), anyString(), any())).thenReturn("mock-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8443/auth/callback?token=mock-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void onAuthenticationSuccess_MissingEmailGithub_ShouldFetchEmail() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123);
        attributes.put("name", "Github User");

        OAuth2User principal = new DefaultOAuth2User(List.of(), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, List.of(), "github");

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("gh-token");
        when(client.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientService.loadAuthorizedClient(eq("github"), anyString())).thenReturn(client);

        // Mock RestClient calls
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri("https://api.github.com/user/emails")).thenReturn(headersSpec);
        when(headersSpec.headers(any(Consumer.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        List<Map<String, Object>> emails = List.of(
            Map.of("email", "primary@example.com", "primary", true, "verified", true)
        );
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(emails);

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(userProvisioningService.provision(eq("github"), eq("123"), eq("primary@example.com"), anyString(), anyString(), any()))
            .thenReturn(new UserProvisioningService.ProvisionedUser(userId, accountId, "Test Account", Collections.emptySet()));
        when(jwtService.createToken(any(), anyString(), any())).thenReturn("mock-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8443/auth/callback?token=mock-token");
    }

    @Test
    void onAuthenticationSuccess_MissingEmailNotGithub_ShouldRedirectWithError() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "123");
        attributes.put("name", "Other User");

        OAuth2User principal = new DefaultOAuth2User(List.of(), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, List.of(), "other-provider");

        when(userProvisioningService.provision(anyString(), anyString(), eq(null), anyString(), anyString(), any()))
            .thenThrow(new MissingEmailException("Email is required"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(org.mockito.ArgumentMatchers.contains("error=missing_email"));
    }
}
