/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.security;

import static java.util.List.of;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.service.ApiTokenService;


/**
 * ApiTokenAuthFilter is a Spring Security filter that processes each incoming HTTP request
 * to authenticate users based on an API token provided in the request headers.
 * This filter looks for the API token in the "Authorization" header (prefixed with "Bearer ")
 * or the "X-API-Token" header. The token is then validated using the ApiTokenService, and if
 * valid, the corresponding user ID is extracted and set in the Security Context.
 * This filter ensures that any valid request carrying an API token will have the appropriate
 * authentication details set for use during subsequent processing.
 * The filter is executed once per request and extends the OncePerRequestFilter class provided
 * by the Spring Framework.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    private final ApiTokenService apiTokenService;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException {
        try {
            final var header = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = null;
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
            if (!StringUtils.hasText(token)) {
                token = request.getHeader("X-API-Token");
            }

            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                final var userIdOpt = apiTokenService.validateAndGetUserId(token);
                if (userIdOpt.isPresent()) {
                    final var userId = userIdOpt.get();
                    final var auth = new UsernamePasswordAuthenticationToken(
                        userId, null, of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (final Exception e) {
            log.debug("API token auth error: {}", e.toString());
        }
        filterChain.doFilter(request, response);
    }
}
