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

import java.util.Locale;

import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Matches exchanges addressed to the Port Buddy main domain itself (dashboard, control-plane API, OAuth).
 *
 * <p>Requests for tunnel subdomains ({@code <name>.<domain>}) and for custom domains do <b>not</b> match. Those
 * carry end-user traffic destined for an exposed service, so the gateway must never apply its own
 * authentication or response-header policy to them. Any {@code Authorization} header on such a request belongs
 * to the exposed application, not to Port Buddy.</p>
 */
public class MainDomainExchangeMatcher implements ServerWebExchangeMatcher {

    private final String mainHost;

    /**
     * Creates a matcher for the configured application domain.
     *
     * @param domain the value of {@code app.domain}; an optional {@code :port} suffix is ignored
     */
    public MainDomainExchangeMatcher(final String domain) {
        this.mainHost = stripPort(domain);
    }

    @Override
    public Mono<MatchResult> matches(final ServerWebExchange exchange) {
        return isMainDomain(exchange) ? MatchResult.match() : MatchResult.notMatch();
    }

    /**
     * Decides whether the exchange targets the main domain.
     *
     * <p>A request without any resolvable host is treated as targeting the main domain, so that the stricter
     * security chain applies by default.</p>
     *
     * @param exchange the current exchange
     * @return {@code true} if the request host equals the configured main domain
     */
    public boolean isMainDomain(final ServerWebExchange exchange) {
        final var request = exchange.getRequest();
        final var hostHeader = request.getHeaders().getHost();
        var host = hostHeader != null ? hostHeader.getHostString() : request.getURI().getHost();
        if (host == null || host.isBlank()) {
            return true;
        }
        host = stripPort(host);
        return host.equals(mainHost);
    }

    private static String stripPort(final String value) {
        var result = value.trim();
        // IPv6 literal such as [::1]:8443 — keep the bracketed address, drop the port.
        if (result.startsWith("[")) {
            final var end = result.indexOf(']');
            if (end > 0) {
                result = result.substring(0, end + 1);
            }
        } else {
            final var colon = result.indexOf(':');
            if (colon > 0) {
                result = result.substring(0, colon);
            }
        }
        return result.toLowerCase(Locale.ROOT);
    }
}
