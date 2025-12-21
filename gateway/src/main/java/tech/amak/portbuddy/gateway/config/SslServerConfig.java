/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tech.amak.portbuddy.gateway.ssl.DynamicSslProvider;
import tech.amak.portbuddy.gateway.ssl.SniSslContextMapping;

@Configuration
public class SslServerConfig {

    private final AppProperties properties;
    private final DynamicSslProvider sslProvider;
    private final HttpHandler httpHandler;
    private DisposableServer httpServer;

    public SslServerConfig(final AppProperties properties,
                           final DynamicSslProvider sslProvider,
                           final HttpHandler httpHandler) {
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
    public NettyServerCustomizer sslCustomizer() {
        return server -> {
            if (properties.ssl() != null && properties.ssl().enabled()) {
                return server.doOnConnection(connection -> 
                    connection.addHandlerFirst(new SniHandler(new SniSslContextMapping(sslProvider))));
            }
            return server;
        };
    }

    /**
     * Starts an additional HTTP server when SSL is enabled.
     * It handles ACME challenges and redirects other requests to HTTPS.
     */
    @PostConstruct
    public void startHttpServer() {
        if (properties.ssl() != null && properties.ssl().enabled()) {
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
