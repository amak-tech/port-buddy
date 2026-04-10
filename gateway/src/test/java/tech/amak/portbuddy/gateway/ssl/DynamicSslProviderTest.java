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

package tech.amak.portbuddy.gateway.ssl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.client.SslServiceClient;
import tech.amak.portbuddy.gateway.config.AppProperties;
import tech.amak.portbuddy.gateway.dto.CertificateResponse;

@ExtendWith(MockitoExtension.class)
class DynamicSslProviderTest {

    @Mock
    private SslServiceClient sslServiceClient;

    @Mock
    private AppProperties properties;

    @Mock
    private AppProperties.Ssl sslProperties;

    @TempDir
    File tempDir;

    private DynamicSslProvider sslProvider;

    @BeforeEach
    void setUp() {
        when(properties.domain()).thenReturn("portbuddy.dev");
        when(properties.ssl()).thenReturn(sslProperties);
        when(sslProperties.fallback()).thenReturn(null);
        sslProvider = new DynamicSslProvider(sslServiceClient, properties);
    }

    @Test
    void shouldReturnFallbackWhenCertificateNotFound() {
        // Given
        when(sslServiceClient.getCertificate(anyString())).thenReturn(Mono.empty());

        // When
        final SslContext context = sslProvider.getSslContext("UNKNOWN.COM").block();

        // Then
        assertNotNull(context);
    }

    @Test
    void shouldReturnFallbackWhenHostnameIsNull() {
        // When
        final SslContext context = sslProvider.getSslContext(null).block();

        // Then
        assertNotNull(context);
    }

    @Test
    void shouldLoadSslContextFromFile() throws Exception {
        // Given
        final SelfSignedCertificate ssc = new SelfSignedCertificate();
        final String hostname = "test.portbuddy.dev";
        final CertificateResponse response = new CertificateResponse(
            hostname,
            ssc.certificate().getAbsolutePath(),
            ssc.privateKey().getAbsolutePath(),
            null,
            null
        );
        when(sslServiceClient.getCertificate("*.portbuddy.dev")).thenReturn(Mono.just(response));

        // When
        final SslContext context = sslProvider.getSslContext(hostname).block();

        // Then
        assertNotNull(context);
        assertTrue(context.isServer());
    }
}
