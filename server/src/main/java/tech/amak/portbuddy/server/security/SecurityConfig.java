package tech.amak.portbuddy.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiTokenAuthFilter apiTokenAuthFilter;
    private final AppProperties properties;
    private final Oauth2SuccessHandler oauth2SuccessHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/token-exchange").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiTokenAuthFilter, BearerTokenAuthenticationFilter.class)
            // Enforce stateless API: no HTTP session will be created or used for authentication
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Store SecurityContext only in the request, never in an HTTP session or cookie
            .securityContext(sc -> sc.securityContextRepository(new RequestAttributeSecurityContextRepository()))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .logout(logout -> logout
                // Logout for stateless API: handle POST /api/auth/logout on this server (relative URL)
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .permitAll()
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/assets/**", "/favicon.*",
                    "/actuator/health**", "/ingress/**", "/ws/**", "/oauth2/**", "/login**",
                    "/.well-known/jwks.json"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth -> oauth
                // OAuth2 is used only to mint a JWT and redirect back. API calls must use Authorization: Bearer <token>.
                .successHandler(oauth2SuccessHandler)
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // By default, no authorities mapping needed; keep empty or map a default role if required.
        return new JwtAuthenticationConverter();
    }
}
