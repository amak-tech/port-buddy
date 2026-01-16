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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .authorizeExchange(exchange -> exchange
                // Public endpoints (static, SPA, OAuth callbacks, JWKS, etc.)
                .pathMatchers(
                    "/", "/index.html", "/assets/**", "/favicon.*",
                    "/install", "/docs", "/privacy", "/terms", "/register", "/passcode",
                    "/app/**", "/login/**", "/auth/callback",
                    "/forgot-password**", "/reset-password**",
                    "/oauth2/**", "/login/oauth2/**",
                    "/.well-known/jwks.json",
                    // Token exchange must be public to let CLI obtain a JWT
                    "/api/auth/token-exchange", "/api/auth/login", "/api/auth/register",
                    "/api/auth/password-reset/**", "/api/webhooks/stripe"
                    ).permitAll()
                // Secure API endpoints
                .pathMatchers("/api/**").authenticated()
                // Everything else is allowed (e.g., subdomain ingress and public tunnels)
                .anyExchange().permitAll()
            )
            // Validate bearer tokens for secured endpoints
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
