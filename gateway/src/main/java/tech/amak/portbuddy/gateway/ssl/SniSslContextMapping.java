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
        sslProvider.getSslContext(hostname)
            .subscribe(
                promise::setSuccess,
                promise::setFailure
            );
        return promise;
    }
}
