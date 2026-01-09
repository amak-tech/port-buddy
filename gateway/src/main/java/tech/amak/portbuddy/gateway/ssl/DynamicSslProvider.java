/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.ssl;

import java.io.File;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.client.SslServiceClient;
import tech.amak.portbuddy.gateway.config.AppProperties;

@Service
@Slf4j
public class DynamicSslProvider {

    private final AppProperties properties;
    private final SslServiceClient sslServiceClient;
    private final AsyncCache<String, SslContext> sslContextCache;
    private final String baseDomain;
    private final SslContext fallbackSslContext;

    /**
     * Constructs a new instance of the DynamicSslProvider.
     *
     * @param sslServiceClient an instance of SslServiceClient used to communicate with the SSL service
     * @param properties       an instance of AppProperties containing configuration values
     */
    public DynamicSslProvider(final SslServiceClient sslServiceClient, final AppProperties properties) {
        this.sslServiceClient = sslServiceClient;
        this.properties = properties;
        this.baseDomain = properties.domain();
        this.sslContextCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .buildAsync();
        this.fallbackSslContext = createFallbackSslContext();
    }

    private SslContext createFallbackSslContext() {
        final var fallback = properties.ssl().fallback();

        try {
            if (fallback == null || !fallback.enabled()) {
                log.info("Fallback certificate is disabled. Generating a temporary self-signed certificate.");
                final var ssc = new SelfSignedCertificate();
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            }

            log.info("Loading fallback certificate from: {} and {}",
                fallback.keyCertChainFile(), fallback.keyFile());

            try (var certStream = fallback.keyCertChainFile().getInputStream();
                 var keyStream = fallback.keyFile().getInputStream()) {
                return SslContextBuilder.forServer(certStream, keyStream).build();
            }
        } catch (final Exception e) {
            log.error("Failed to create fallback SSL context", e);
            try {
                final var ssc = new SelfSignedCertificate();
                return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (final Exception ex) {
                log.error("Failed to create even a temporary self-signed certificate", ex);
                return null;
            }
        }
    }

    public SslContext getFallbackSslContext() {
        return fallbackSslContext;
    }

    /**
     * Retrieves SslContext for a given hostname, utilizing Caffeine cache.
     *
     * @param hostname requested hostname
     * @return Mono of SslContext or fallback if not found
     */
    public Mono<SslContext> getSslContext(final String hostname) {
        if (hostname == null) {
            return Mono.just(fallbackSslContext);
        }
        return Mono.fromFuture(sslContextCache.get(hostname, (h, executor) -> loadSslContext(h).toFuture()));
    }

    private Mono<SslContext> loadSslContext(final String hostname) {
        var lookupDomain = hostname;
        if (hostname.endsWith("." + baseDomain)) {
            lookupDomain = "*." + baseDomain;
        }

        log.debug("Loading SSL context for hostname: {}, lookup domain: {}", hostname, lookupDomain);

        final String finalLookupDomain = lookupDomain;
        return sslServiceClient.getCertificate(lookupDomain)
            .map(cert -> {
                if (cert == null || cert.certificatePath() == null || cert.privateKeyPath() == null) {
                    log.warn("No certificate found for {}. Using fallback.", finalLookupDomain);
                    return fallbackSslContext;
                }

                try {
                    return SslContextBuilder.forServer(
                        new File(cert.privateKeyPath()),
                        new File(cert.certificatePath())
                    ).build();
                } catch (final Exception e) {
                    log.error("Failed to create SslContext for {}. Using fallback.", finalLookupDomain, e);
                    return fallbackSslContext;
                }
            })
            .defaultIfEmpty(fallbackSslContext)
            .onErrorResume(e -> {
                log.error("Error retrieving certificate for {}. Using fallback.", finalLookupDomain, e);
                return Mono.just(fallbackSslContext);
            });
    }
}
