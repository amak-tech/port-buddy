/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.ssl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.handler.ssl.SslContext;
import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.client.SslServiceClient;
import tech.amak.portbuddy.gateway.config.AppProperties;

@ExtendWith(MockitoExtension.class)
class DynamicSslProviderTest {

    @Mock
    private SslServiceClient sslServiceClient;

    @Mock
    private AppProperties properties;

    private DynamicSslProvider sslProvider;

    @BeforeEach
    void setUp() {
        when(properties.domain()).thenReturn("portbuddy.dev");
        sslProvider = new DynamicSslProvider(sslServiceClient, properties);
    }

    @Test
    void shouldReturnFallbackWhenCertificateNotFound() {
        // Given
        when(sslServiceClient.getCertificate(anyString())).thenReturn(Mono.empty());

        // When
        final SslContext context = sslProvider.getSslContext("unknown.com");

        // Then
        assertNotNull(context);
    }

    @Test
    void shouldReturnFallbackWhenHostnameIsNull() {
        // When
        final SslContext context = sslProvider.getSslContext(null);

        // Then
        assertNotNull(context);
    }
}
