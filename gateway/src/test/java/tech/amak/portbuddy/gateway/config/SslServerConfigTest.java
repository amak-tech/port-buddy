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

package tech.amak.portbuddy.gateway.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import javax.net.ssl.SSLException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tech.amak.portbuddy.gateway.ssl.DynamicSslProvider;

class SslServerConfigTest {

    @Test
    void shouldInvokeDynamicSslProviderOnSniHandshake() throws Exception {
        // Given
        final var sslProvider = mock(DynamicSslProvider.class);
        final var properties = mock(AppProperties.class);
        final var sslProperties = mock(AppProperties.Ssl.class);
        final var httpHandler = mock(HttpHandler.class);

        final var ssc = new SelfSignedCertificate();
        final var fallbackContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        when(properties.ssl()).thenReturn(sslProperties);
        when(sslProperties.enabled()).thenReturn(true);
        when(sslProvider.getFallbackSslContext()).thenReturn(fallbackContext);
        // Return fallback even for dynamic to avoid complex setup, we just want to see if it's called
        when(sslProvider.getSslContext(anyString())).thenReturn(Mono.just(fallbackContext));
        when(httpHandler.handle(any(), any())).thenReturn(Mono.empty());

        final var sslServerConfig = new SslServerConfig(properties, sslProvider, httpHandler);
        final var customizer = sslServerConfig.sslCustomizer();

        final var factory = new NettyReactiveWebServerFactory(0);
        customizer.customize(factory);

        final var webServer = factory.getWebServer(httpHandler);
        webServer.start();
        try {
            final int port = webServer.getPort();

            // When - attempt a connection with SNI
            HttpClient.create()
                .port(port)
                .remoteAddress(() -> new java.net.InetSocketAddress("127.0.0.1", port))
                .secure(spec -> {
                    try {
                        spec.sslContext(SslContextBuilder.forClient()
                                .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                                .build())
                            .serverNames(new javax.net.ssl.SNIHostName("test.portbuddy.dev"));
                    } catch (final SSLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .get()
                .uri("/")
                .response()
                .block(Duration.ofSeconds(5));
        } catch (final Exception e) {
            // It might fail for various reasons (no actual handler for the request), but we care about SNI lookup
        } finally {
            webServer.stop();
        }

        // Then
        verify(sslProvider, atLeastOnce()).getSslContext("test.portbuddy.dev");
    }
}
