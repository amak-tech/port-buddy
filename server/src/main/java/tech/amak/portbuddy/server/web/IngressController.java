package tech.amak.portbuddy.server.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

/**
 * HTTP ingress that forwards requests to a client tunnel by subdomain.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IngressController {

    private final TunnelRegistry registry;

    // Path-based fallback ingress for local/dev: http://server/_/{subdomain}/...
    @RequestMapping("/_/{subdomain}/**")
    public void ingressPathBased(final @PathVariable("subdomain") String subdomain,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) throws IOException {
        forwardViaTunnel(subdomain, request, response);
    }

    private void forwardViaTunnel(final String subdomain,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws IOException {
        final var pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final var bestMatch = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        final var matcher = new AntPathMatcher();
        var path = matcher.extractPathWithinPattern(bestMatch, pathWithin);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        final var method = request.getMethod();
        final var query = request.getQueryString();

        final Map<String, String> headers = new HashMap<>();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
            final var name = en.nextElement();
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
                for (var header : resp.getRespHeaders().entrySet()) {
                    // avoid sending null header values
                    if (header.getValue() != null) {
                        response.setHeader(header.getKey(), header.getValue());
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
