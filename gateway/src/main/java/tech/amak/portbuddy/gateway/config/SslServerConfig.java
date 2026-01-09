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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tech.amak.portbuddy.gateway.ssl.DynamicSslProvider;
import tech.amak.portbuddy.gateway.ssl.SniSslContextMapping;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SslServerConfig {

    private final AppProperties properties;
    private final DynamicSslProvider sslProvider;
    private final HttpHandler httpHandler;
    private DisposableServer httpServer;

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
                // We use doOnChannelInit to configure the pipeline at the transport level.
                // This ensures SniHandler is added before any data is read and enables dynamic SSL via SNI.
                server = server.doOnChannelInit((observer, channel, remoteAddress) -> {
                    channel.pipeline().addFirst("sni-handler", new SniHandler(new SniSslContextMapping(sslProvider)));
                });
            }

            return server.httpRequestDecoder(spec -> spec.allowDuplicateContentLengths(true)
                .maxInitialLineLength(65536)
                .maxHeaderSize(65536)
                .maxChunkSize(65536)
                .validateHeaders(false));
        });
    }

    /**
     * Starts an HTTP server to handle incoming requests. If SSL is enabled, the server redirects
     * non-secure requests to the HTTPS endpoint. The server also handles requests to the ACME
     * challenge endpoint for SSL certificate validation.
     * Behavior details:
     * - If SSL is enabled:
     * - Requests to paths starting with "/.well-known/acme-challenge/" are processed directly by
     * the {@code httpHandler} via the {@code ReactorHttpHandlerAdapter}.
     * - Requests to other paths:
     * - If the request does not include a "Host" header, a {@code 400 Bad Request} response
     * is returned.
     * - If the "Host" header is present, the server constructs a redirect URL based on the
     * secure port provided in the SSL properties. Non-secure requests are redirected to
     * their HTTPS equivalents with an appropriate {@code 301 Moved Permanently} response.
     * Prerequisites:
     * - The server requires SSL properties configuration to determine if SSL is enabled and to
     * identify the secure port for redirections.
     * - The {@code httpHandler} must be set up to process ACME challenge requests or other
     * required application logic.
     * This method is annotated with {@code @PostConstruct}, ensuring it is executed automatically
     * during the initialization phase of the containing class.
     */
    @PostConstruct
    public void startHttpServer() {
        if (properties.ssl().enabled()) {
            final var adapter = new ReactorHttpHandlerAdapter(httpHandler);
            this.httpServer = HttpServer.create()
                .port(properties.httpPort())
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

                        final var redirectUrl = properties.url() + path;

                        response.status(HttpStatus.MOVED_PERMANENTLY.value());
                        response.header(HttpHeaderNames.LOCATION, redirectUrl);
                        return response.send();
                    }
                })
                .bindNow();
        }
    }

    /**
     * Stops the currently running HTTP server, if it is initialized.
     * This method is invoked automatically when the containing class is being destroyed,
     * as indicated by the {@code @PreDestroy} annotation. It ensures proper release of resources
     * by shutting down the HTTP server. If no server instance is present, the method exits quietly.
     * Behavior:
     * - If the {@code httpServer} is non-null, it invokes {@code disposeNow()} to stop the server immediately.
     * Prerequisites:
     * - A valid {@code httpServer} instance must exist for this method to perform the shutdown process.
     * If the instance is null, the method performs no action.
     * Usage context:
     * - Typically used in applications with lifecycle management to ensure clean shutdown
     * and resource deallocation.
     */
    @PreDestroy
    public void stopHttpServer() {
        if (this.httpServer != null) {
            this.httpServer.disposeNow();
        }
    }
}
