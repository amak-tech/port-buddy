/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SniHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tech.amak.portbuddy.gateway.ssl.DynamicSslProvider;
import tech.amak.portbuddy.gateway.ssl.SniSslContextMapping;

@Configuration
@Slf4j
public class SslServerConfig {

    private final AppProperties properties;
    private final DynamicSslProvider sslProvider;
    private final HttpHandler httpHandler;
    private DisposableServer httpServer;

    public SslServerConfig(final AppProperties properties,
                           final DynamicSslProvider sslProvider,
                           final HttpHandler httpHandler) {
        log.info("SslServerConfig initialized with properties: {}", properties);
        this.properties = properties;
        this.sslProvider = sslProvider;
        this.httpHandler = httpHandler;
    }

    /**
     * Customizes Netty server to support dynamic SSL termination via SNI.
     * The main server will be SSL-enabled.
     *
     * @return NettyServerCustomizer
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> sslCustomizer() {
        return factory -> factory.addServerCustomizers(server -> {
            if (properties.ssl().enabled()) {
                server = server.secure(sslContextSpec ->
                    sslContextSpec.sslContext(sslProvider.getFallbackSslContext()));

                // Add SNI support at the connection level to complement the secure() configuration
                server = server.doOnConnection(connection -> {
                    connection.addHandlerFirst("sni-handler", new SniHandler(new SniSslContextMapping(sslProvider)));
                });
            }
            return server.httpRequestDecoder(spec -> spec.allowDuplicateContentLengths(true)
                .maxInitialLineLength(65536)
                .maxHeaderSize(65536)
                .maxChunkSize(65536)
                .validateHeaders(false));
        });
    }

    @PostConstruct
    public void startHttpServer() {
        if (properties.ssl().enabled()) {
            final var adapter = new ReactorHttpHandlerAdapter(httpHandler);
            this.httpServer = HttpServer.create()
                .port(8080)
                .handle((request, response) -> {
                    final var path = request.uri();
                    if (path.startsWith("/.well-known/acme-challenge/")) {
                        return adapter.apply(request, response);
                    } else {
                        final var host = request.requestHeaders().get(HttpHeaderNames.HOST);
                        if (host == null) {
                            response.status(HttpStatus.BAD_REQUEST.value());
                            return response.send();
                        }

                        // Remove port from host if present
                        final var hostWithoutPort = host.contains(":") ? host.substring(0, host.indexOf(":")) : host;
                        final var sslPort = properties.ssl().port();
                        final var redirectUrl = "https://" + hostWithoutPort
                                                + (sslPort == 443 ? "" : ":" + sslPort) + path;

                        response.status(HttpStatus.MOVED_PERMANENTLY.value());
                        response.header(HttpHeaderNames.LOCATION, redirectUrl);
                        return response.send();
                    }
                })
                .bindNow();
        }
    }

    @PreDestroy
    public void stopHttpServer() {
        if (this.httpServer != null) {
            this.httpServer.disposeNow();
        }
    }
}
