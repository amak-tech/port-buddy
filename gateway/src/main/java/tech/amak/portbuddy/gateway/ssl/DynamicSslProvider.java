/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.ssl;

import java.io.File;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.gateway.client.SslServiceClient;
import tech.amak.portbuddy.gateway.config.AppProperties;
import tech.amak.portbuddy.gateway.dto.CertificateResponse;

@Service
@Slf4j
public class DynamicSslProvider {

    private final SslServiceClient sslServiceClient;
    private final Cache<String, SslContext> sslContextCache;
    private final String baseDomain;
    private final SslContext fallbackSslContext;

    public DynamicSslProvider(final SslServiceClient sslServiceClient, final AppProperties properties) {
        this.sslServiceClient = sslServiceClient;
        this.baseDomain = properties.domain();
        this.sslContextCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
        this.fallbackSslContext = createFallbackSslContext();
    }

    private SslContext createFallbackSslContext() {
        try {
            final var ssc = new SelfSignedCertificate();
            return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } catch (final Exception e) {
            log.error("Failed to create fallback self-signed certificate", e);
            return null;
        }
    }

    /**
     * Retrieves SslContext for a given hostname, utilizing Caffeine cache.
     *
     * @param hostname requested hostname
     * @return SslContext or fallback if not found
     */
    public SslContext getSslContext(final String hostname) {
        if (hostname == null) {
            return fallbackSslContext;
        }
        return sslContextCache.get(hostname, this::loadSslContext);
    }

    private SslContext loadSslContext(final String hostname) {
        String lookupDomain = hostname;
        if (hostname.endsWith("." + baseDomain)) {
            lookupDomain = "*." + baseDomain;
        }

        log.debug("Loading SSL context for hostname: {}, lookup domain: {}", hostname, lookupDomain);

        try {
            final CertificateResponse cert = sslServiceClient.getCertificate(lookupDomain).block(Duration.ofSeconds(5));

            if (cert == null || cert.certificatePath() == null || cert.privateKeyPath() == null) {
                log.warn("No certificate found for {}. Using fallback.", lookupDomain);
                return fallbackSslContext;
            }

            return SslContextBuilder.forServer(
                new File(cert.privateKeyPath()),
                new File(cert.certificatePath())
            ).build();
        } catch (final Exception e) {
            log.error("Failed to create SslContext for {}. Using fallback.", lookupDomain, e);
            return fallbackSslContext;
        }
    }
}
