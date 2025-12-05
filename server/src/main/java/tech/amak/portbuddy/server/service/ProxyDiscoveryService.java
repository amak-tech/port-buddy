/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProxyDiscoveryService {

    public static final String SERVICE_ID = "net-proxy";

    private final DiscoveryClient discoveryClient;

    /**
     * Returns a list of public hosts of all available tcp-proxy instances registered in Eureka.
     * The public host is resolved from instance metadata keys in the following order:
     * - "public-host"
     * - "publicHost"
     * - "app.public-host"
     * - "app.publicHost"
     * Falls back to {@link ServiceInstance#getHost()} if none present.
     */
    public List<String> listPublicHosts() {
        final var instances = discoveryClient.getInstances(SERVICE_ID);
        final Set<String> hosts = new LinkedHashSet<>();
        for (final ServiceInstance instance : instances) {
            final var md = instance.getMetadata();
            String host = null;
            if (md != null) {
                host = firstNonBlank(
                    md.get("public-host"),
                    md.get("publicHost"),
                    md.get("app.public-host"),
                    md.get("app.publicHost")
                );
            }
            if (host == null || host.isBlank()) {
                host = instance.getHost();
            }
            if (host != null && !host.isBlank()) {
                hosts.add(host);
            }
        }
        return hosts.stream().collect(Collectors.toList());
    }

    private String firstNonBlank(final String... values) {
        for (final String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
