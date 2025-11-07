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

/**
 * Intercepts requests with Host like "{sub}.port-buddy.com" and forwards them via tunnel.
 * Skips /api and /actuator endpoints. Lets WebSocket Upgrade pass to WS handlers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HostTunnelFilter extends OncePerRequestFilter {

    private final TunnelForwarder forwarder;

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
        final var upgrade = request.getHeader("Upgrade");
        if (upgrade != null && "websocket".equalsIgnoreCase(upgrade)) {
            filterChain.doFilter(request, response);
            return;
        }

        final var host = request.getHeader(HttpHeaders.HOST);
        if (host != null && host.endsWith(".port-buddy.com") && host.indexOf('.') > 0) {
            final var firstDot = host.indexOf('.');
            final var subdomain = host.substring(0, firstDot);
            forwarder.forwardHostBased(subdomain, request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
