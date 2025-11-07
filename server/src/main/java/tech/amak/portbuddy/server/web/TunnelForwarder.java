package tech.amak.portbuddy.server.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

@Slf4j
@Component
@RequiredArgsConstructor
public class TunnelForwarder {

    private final TunnelRegistry registry;

    public void forwardViaTunnel(final String subdomain,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final String pathWithin,
                                 final String bestMatch) throws IOException {
        final var matcher = new AntPathMatcher();
        var path = matcher.extractPathWithinPattern(bestMatch, pathWithin);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        forward(subdomain, request, response, path);
    }

    public void forwardHostBased(final String subdomain,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) throws IOException {
        var path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        forward(subdomain, request, response, path);
    }

    private void forward(final String subdomain,
                         final HttpServletRequest request,
                         final HttpServletResponse response,
                         final String path) throws IOException {
        final var method = request.getMethod();
        final var query = request.getQueryString();

        final Map<String, String> headers = new HashMap<>();
        for (final Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
            final var name = headerNames.nextElement();
            // Skip hop-by-hop headers
            if (name.equalsIgnoreCase(HttpHeaders.HOST) || name.equalsIgnoreCase(HttpHeaders.CONNECTION)) {
                continue;
            }
            headers.put(name, request.getHeader(name));
        }

        headers.put("X-Forwarded-Host", request.getServerName());
        headers.put("X-Forwarded-Proto", request.isSecure() ? "https" : "http");

        final var bodyBytes = request.getInputStream().readAllBytes();
        final var bodyB64 = bodyBytes.length == 0 ? null : Base64.getEncoder().encodeToString(bodyBytes);

        final var msg = new HttpTunnelMessage();
        msg.setMethod(method);
        msg.setPath(path);
        msg.setQuery(query);
        msg.setHeaders(headers);
        msg.setBodyB64(bodyB64);

        try {
            final var resp = registry.forwardRequest(subdomain, msg, Duration.ofSeconds(30)).join();
            final var status = resp.getStatus() == null ? 502 : resp.getStatus();
            response.setStatus(status);
            if (resp.getRespHeaders() != null) {
                for (var e : resp.getRespHeaders().entrySet()) {
                    if (e.getValue() != null) {
                        response.setHeader(e.getKey(), e.getValue());
                    }
                }
            }
            if (resp.getRespBodyB64() != null) {
                final var bytes = Base64.getDecoder().decode(resp.getRespBodyB64());
                response.getOutputStream().write(bytes);
            }
        } catch (final Exception ex) {
            log.warn("Tunnel forward failed for subdomain={}: {}", subdomain, ex.toString());
            response.setStatus(502);
            response.getWriter().write("Bad Gateway: tunnel unavailable");
        }
    }
}
