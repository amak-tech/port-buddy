/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.ssl;

import java.io.File;
import java.time.Duration;

import javax.net.ssl.SSLException;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
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

    public DynamicSslProvider(final SslServiceClient sslServiceClient, final AppProperties properties) {
        this.sslServiceClient = sslServiceClient;
        this.baseDomain = properties.domain();
        this.sslContextCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
    }

    /**
     * Retrieves SslContext for a given hostname, utilizing Caffeine cache.
     *
     * @param hostname requested hostname
     * @return SslContext or null if not found
     */
    public SslContext getSslContext(final String hostname) {
        if (hostname == null) {
            return null;
        }
        return sslContextCache.get(hostname, this::loadSslContext);
    }

    private SslContext loadSslContext(final String hostname) {
        String lookupDomain = hostname;
        if (hostname.endsWith("." + baseDomain)) {
            lookupDomain = "*." + baseDomain;
        }

        log.debug("Loading SSL context for hostname: {}, lookup domain: {}", hostname, lookupDomain);

        final CertificateResponse cert = sslServiceClient.getCertificate(lookupDomain).block(Duration.ofSeconds(5));

        if (cert == null || cert.certificatePath() == null || cert.privateKeyPath() == null) {
            log.warn("No certificate found for {}", lookupDomain);
            return null;
        }

        try {
            return SslContextBuilder.forServer(
                new File(cert.privateKeyPath()),
                new File(cert.certificatePath())
            ).build();
        } catch (final SSLException e) {
            log.error("Failed to create SslContext for {}", lookupDomain, e);
            return null;
        }
    }
}
