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

package tech.amak.portbuddy.gateway.filter;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class PortBuddyRewritePathGatewayFilterFactory extends RewritePathGatewayFilterFactory {

    @Override
    public GatewayFilter apply(final Config config) {

        final var replacement = config.getReplacement().replace("$\\", "$");
        final var pattern = Pattern.compile(config.getRegexp());

        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
                final var request = exchange.getRequest();
                addOriginalRequestUrl(exchange, request.getURI());
                final var path = request.getURI().getRawPath();

                final var replacementReference = new AtomicReference<>(replacement);

                final var uriTemplateVariables = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
                uriTemplateVariables.forEach((key, value) -> {
                    final var placeholder = "${%s}".formatted(key);
                    var cleanValue = value;
                    if ("customDomain".equals(key)) {
                        final var colonIdx = cleanValue.indexOf(':');
                        if (colonIdx > 0) {
                            cleanValue = cleanValue.substring(0, colonIdx);
                        }
                    }
                    replacementReference.set(replacementReference.get().replace(placeholder, cleanValue));
                });

                // Fallback for customDomain and support for host if they are not in uriTemplateVariables
                if (replacementReference.get().contains("${customDomain}")
                    || replacementReference.get().contains("${host}")) {
                    var host = request.getHeaders().getFirst("Host");
                    if (host != null) {
                        final var colonIdx = host.indexOf(':');
                        if (colonIdx > 0) {
                            host = host.substring(0, colonIdx);
                        }
                        replacementReference.set(replacementReference.get().replace("${customDomain}", host));
                        replacementReference.set(replacementReference.get().replace("${host}", host));
                    }
                }

                final var newPath = pattern.matcher(path).replaceAll(replacementReference.get());

                final var mutatedRequest = request.mutate().path(newPath).build();

                exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, mutatedRequest.getURI());

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }

            @Override
            public String toString() {
                return filterToStringCreator(PortBuddyRewritePathGatewayFilterFactory.this)
                    .append(config.getRegexp(), replacement)
                    .toString();
            }
        };
    }
}
