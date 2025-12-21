/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.util.AsyncMapping;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SniSslContextMapping implements AsyncMapping<String, SslContext> {

    private final DynamicSslProvider sslProvider;

    @Override
    public Future<SslContext> map(final String hostname, final Promise<SslContext> promise) {
        log.debug("SNI lookup for hostname: {}", hostname);
        try {
            final SslContext sslContext = sslProvider.getSslContext(hostname);
            if (sslContext != null) {
                return promise.setSuccess(sslContext);
            } else {
                return promise.setFailure(new RuntimeException("No SSL context found for " + hostname));
            }
        } catch (final Exception e) {
            log.error("Error during SNI lookup for {}", hostname, e);
            return promise.setFailure(e);
        }
    }
}
