package tech.amak.portbuddy.server.web;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;

/**
 * Intercepts requests with Host like "{sub}.portbuddy.dev" and forwards them via tunnel.
 * Skips /api and /actuator endpoints. Lets WebSocket Upgrade pass to WS handlers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HostTunnelFilter extends OncePerRequestFilter {

    private static final String UPGRADE_HEADER = "Upgrade";
    private static final String UPGRADE_TO_WEBSOCKET = "Upgrade";

    private final TunnelForwarder forwarder;
    private final AppProperties properties;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException {
        final var uri = request.getRequestURI();
        if (uri.startsWith("/api") || uri.startsWith("/actuator") || uri.startsWith("/_/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Let WebSocket upgrade go through to WebSocket handlers
        final var upgrade = request.getHeader(UPGRADE_HEADER);
        if (UPGRADE_TO_WEBSOCKET.equalsIgnoreCase(upgrade)) {
            filterChain.doFilter(request, response);
            return;
        }

        final var host = request.getHeader(HttpHeaders.HOST);
        if (host != null && host.endsWith(properties.gateway().subdomainHost()) && host.indexOf('.') > 0) {
            final var firstDot = host.indexOf('.');
            final var subdomain = host.substring(0, firstDot);
            forwarder.forwardHostBased(subdomain, request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
