/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.server.security;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
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
        handler = new Oauth2SuccessHandler(
            jwtService,
            properties,
            userProvisioningService,
            authorizedClientService,
            restClientBuilder);

        // AppProperties config
        final var gateway = mock(AppProperties.Gateway.class);
        when(properties.gateway()).thenReturn(gateway);
        when(gateway.url()).thenReturn("http://localhost:8443");
    }

    @Test
    void onAuthenticationSuccess_WithEmail_ShouldProvisionAndRedirect() throws IOException {
        final var attributes = new HashMap<String, Object>();
        attributes.put("id", "123");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        final var principal = new DefaultOAuth2User(List.of(), attributes, "id");
        final var authentication = new OAuth2AuthenticationToken(principal, List.of(), "google");

        final var userId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        when(userProvisioningService.provision(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new UserProvisioningService.ProvisionedUser(
                userId, accountId, "Test Account", Collections.emptySet()));
        when(jwtService.createToken(any(), anyString(), any())).thenReturn("mock-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8443/auth/callback?token=mock-token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void onAuthenticationSuccess_MissingEmailGithub_ShouldFetchEmail() throws IOException {
        final var attributes = new HashMap<String, Object>();
        attributes.put("id", 123);
        attributes.put("name", "Github User");

        final var principal = new DefaultOAuth2User(List.of(), attributes, "id");
        final var authentication = new OAuth2AuthenticationToken(principal, List.of(), "github");

        final var client = mock(OAuth2AuthorizedClient.class);
        final var accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("gh-token");
        when(client.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientService.loadAuthorizedClient(eq("github"), anyString())).thenReturn(client);

        // Mock RestClient calls
        final var getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        final var headersSpec = mock(RestClient.RequestHeadersSpec.class);
        final var responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri("https://api.github.com/user/emails")).thenReturn(headersSpec);
        when(headersSpec.headers(any(Consumer.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        final var emails = List.of(
            Map.of("email", "primary@example.com", "primary", true, "verified", true)
        );
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(emails);

        final var userId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        when(userProvisioningService.provision(
            eq("github"), eq("123"), eq("primary@example.com"), anyString(), anyString(), any()))
            .thenReturn(new UserProvisioningService.ProvisionedUser(
                userId, accountId, "Test Account", Collections.emptySet()));
        when(jwtService.createToken(any(), anyString(), any())).thenReturn("mock-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect("http://localhost:8443/auth/callback?token=mock-token");
    }

    @Test
    void onAuthenticationSuccess_MissingEmailNotGithub_ShouldRedirectWithError() throws IOException {
        final var attributes = new HashMap<String, Object>();
        attributes.put("id", "123");
        attributes.put("name", "Other User");

        final var principal = new DefaultOAuth2User(List.of(), attributes, "id");
        final var authentication = new OAuth2AuthenticationToken(principal, List.of(), "other-provider");

        when(userProvisioningService.provision(anyString(), anyString(), eq(null), anyString(), anyString(), any()))
            .thenThrow(new MissingEmailException("Email is required"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(org.mockito.ArgumentMatchers.contains("error=missing_email"));
    }
}
