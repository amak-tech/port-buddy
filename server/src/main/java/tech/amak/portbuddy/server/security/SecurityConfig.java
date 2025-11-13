package tech.amak.portbuddy.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiTokenAuthFilter apiTokenAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final @Value("${app.gateway-url}") String gatewayUrl) throws Exception {
        http
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/assets/**", "/favicon.*",
                    "/actuator/health**", "/ingress/**", "/ws/**", "/oauth2/**", "/login**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/token-exchange").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(apiTokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl(gatewayUrl + "/auth/callback", true)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            );
        return http.build();
    }
}
