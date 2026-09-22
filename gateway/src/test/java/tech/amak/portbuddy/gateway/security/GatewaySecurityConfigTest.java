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

package tech.amak.portbuddy.gateway.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.config.AppProperties;

/**
 * Verifies that gateway security applies only to the main domain and never interferes with tunnel traffic.
 */
@WebFluxTest
@Import({GatewaySecurityConfig.class, GatewaySecurityConfigTest.EchoController.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = "app.domain=portbuddy.dev")
class GatewaySecurityConfigTest {

    private static final String MAIN_HOST = "portbuddy.dev";
    private static final String TUNNEL_HOST = "triel.portbuddy.dev";
    private static final String CUSTOM_HOST = "api.customer-domain.com";
    // Header says HS512; the gateway's RSA/JWKS decoder cannot verify it.
    private static final String FOREIGN_HS512_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkYXRhYmF5In0.sig";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void tunnelSubdomainForwardsForeignBearerTokenOnApiPath() {
        when(jwtDecoder.decode(anyString()))
            .thenReturn(Mono.error(new BadJwtException("Unsupported algorithm of HS512")));

        webTestClient.get().uri(url(TUNNEL_HOST, "/api/v1/label-templates/find?categoryId=1"))
            .header(HttpHeaders.HOST, TUNNEL_HOST)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREIGN_HS512_TOKEN)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE)
            .expectHeader().doesNotExist("X-Frame-Options")
            .expectHeader().doesNotExist("X-Content-Type-Options")
            .expectHeader().doesNotExist(HttpHeaders.CACHE_CONTROL)
            .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void tunnelSubdomainApiPathIsPublicWithoutToken() {
        webTestClient.get().uri(url(TUNNEL_HOST, "/api/v1/anything"))
            .header(HttpHeaders.HOST, TUNNEL_HOST)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void customDomainForwardsForeignBearerToken() {
        when(jwtDecoder.decode(anyString()))
            .thenReturn(Mono.error(new BadJwtException("Unsupported algorithm of HS512")));

        webTestClient.get().uri(url(CUSTOM_HOST, "/api/v1/anything"))
            .header(HttpHeaders.HOST, CUSTOM_HOST)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREIGN_HS512_TOKEN)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE);
    }

    @Test
    void tunnelSubdomainStillAnswersCorsPreflight() {
        webTestClient.options().uri(url(TUNNEL_HOST, "/api/v1/anything"))
            .header(HttpHeaders.HOST, TUNNEL_HOST)
            .header(HttpHeaders.ORIGIN, "https://app.example.com")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com");
    }

    @Test
    void mainDomainApiRequiresAuthentication() {
        webTestClient.get().uri(url(MAIN_HOST, "/api/tunnels"))
            .header(HttpHeaders.HOST, MAIN_HOST)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void mainDomainRejectsInvalidToken() {
        when(jwtDecoder.decode(anyString()))
            .thenReturn(Mono.error(new BadJwtException("Unsupported algorithm of HS512")));

        webTestClient.get().uri(url(MAIN_HOST, "/api/tunnels"))
            .header(HttpHeaders.HOST, MAIN_HOST)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + FOREIGN_HS512_TOKEN)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, ".*invalid_token.*");
    }

    @Test
    void mainDomainAcceptsValidToken() {
        final var now = Instant.now();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(Jwt.withTokenValue("valid")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofHours(1)))
            .build()));

        webTestClient.get().uri(url(MAIN_HOST, "/api/tunnels"))
            .header(HttpHeaders.HOST, MAIN_HOST)
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void mainDomainPublicEndpointsStayPublic() {
        webTestClient.post().uri(url(MAIN_HOST, "/api/auth/login"))
            .header(HttpHeaders.HOST, MAIN_HOST)
            .exchange()
            .expectStatus().isOk();

        webTestClient.get().uri(url(MAIN_HOST, "/docs/http-tunnels"))
            .header(HttpHeaders.HOST, MAIN_HOST)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Frame-Options");
    }

    /**
     * Builds an absolute URL. Reactor Netty always exposes an absolute request URI (derived from the Host
     * header); Spring's CORS same-origin check relies on it, so the mock client must do the same.
     */
    private static String url(final String host, final String path) {
        return "https://" + host + path;
    }

    /**
     * Stand-in for the routed upstream; answers every request with {@code ok}.
     */
    @RestController
    static class EchoController {

        @RequestMapping("/**")
        Mono<String> echo() {
            return Mono.just("ok");
        }
    }
}
