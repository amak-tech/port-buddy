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

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.gateway.config.AppProperties;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final AppProperties properties;

    /**
     * Pass-through chain for tunnel ingress: subdomains of the main domain and custom domains.
     *
     * <p>Traffic on these hosts is forwarded to a user's exposed service, so the gateway applies no
     * authentication (a {@code Bearer} token here is the exposed app's, not ours) and adds no security response
     * headers. Without this chain the resource-server filter would reject any foreign JWT with
     * {@code 401 invalid_token} and {@code /api/**} paths would demand a Port Buddy login.</p>
     *
     * <p>CORS stays enabled with the permissive policy: the gateway's handler mapping rejects pre-flight
     * requests with 403 when no CORS configuration applies, so removing it would break browser clients.</p>
     *
     * @param http the reactive security builder
     * @return the tunnel ingress filter chain
     */
    @Bean
    @Order(1)
    public SecurityWebFilterChain tunnelIngressSecurityFilterChain(final ServerHttpSecurity http) {
        http
            .securityMatcher(new NegatedServerWebExchangeMatcher(new MainDomainExchangeMatcher(properties.domain())))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(ServerHttpSecurity.HeaderSpec::disable)
            .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        return http.build();
    }

    /**
     * Security chain for the main domain: dashboard, control-plane API and OAuth endpoints.
     *
     * @param http the reactive security builder
     * @return the main domain filter chain
     */
    @Bean
    @Order(2)
    public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchange -> exchange
                // Public endpoints (static, SPA, OAuth callbacks, JWKS, etc.)
                .pathMatchers(
                    "/", "/index.html", "/assets/**", "/favicon.*",
                    "/app/**", "/login/**", "/auth/callback",
                    "/forgot-password**", "/reset-password**",
                    "/oauth2/**", "/login/oauth2/**",
                    "/.well-known/jwks.json",
                    // Token exchange must be public to let CLI obtain a JWT
                    "/api/auth/token-exchange", "/api/auth/login", "/api/auth/register",
                    "/api/auth/register/request-otp",
                    "/api/auth/password-reset/**", "/api/webhooks/stripe"
                ).permitAll()
                // Secure API endpoints
                .pathMatchers("/api/**").authenticated()
                // Everything else on the main domain (docs, SPA fallback, ACME challenges) is public
                .anyExchange().permitAll()
            )
            // Validate bearer tokens for secured endpoints
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Configures CORS to allow all origins.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
