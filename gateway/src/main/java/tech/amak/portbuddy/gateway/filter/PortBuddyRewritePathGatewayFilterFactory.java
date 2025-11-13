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
    public GatewayFilter apply(Config config) {

        final var replacement = config.getReplacement().replace("$\\", "$");
        final var pattern = Pattern.compile(config.getRegexp());

        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
                final var request = exchange.getRequest();
                addOriginalRequestUrl(exchange, request.getURI());
                final var path = request.getURI().getRawPath();

                final var replacementReference = new AtomicReference<>(replacement);

                ServerWebExchangeUtils.getUriTemplateVariables(exchange).forEach((key, value) -> {
                    final var placeholder = "${%s}".formatted(key);
                    replacementReference.set(replacementReference.get().replace(placeholder, value));
                });

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
