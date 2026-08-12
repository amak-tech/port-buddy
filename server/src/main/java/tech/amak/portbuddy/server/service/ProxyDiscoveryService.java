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

package tech.amak.portbuddy.server.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.AppProperties;

@Service
@RequiredArgsConstructor
public class ProxyDiscoveryService {

    public static final String SERVICE_ID = "net-proxy";

    private final DiscoveryClient discoveryClient;
    private final AppProperties properties;

    /**
     * A single net-proxy instance as it is offered to the UI: where it is reachable, where it physically
     * sits, and which public ports may be reserved on it.
     *
     * @param host    public host clients connect to
     * @param region  human-readable region of the proxy, {@code null} when the instance does not advertise one
     * @param portMin lowest reservable public port on this proxy, inclusive
     * @param portMax highest reservable public port on this proxy, inclusive
     */
    public record ProxyHost(
        String host,
        String region,
        int portMin,
        int portMax
    ) {
    }

    /**
     * Returns a list of public hosts of all available net-proxy instances registered in Eureka.
     *
     * @return the distinct public hosts, in discovery order
     */
    public List<String> listPublicHosts() {
        return listProxies().stream().map(ProxyHost::host).toList();
    }

    /**
     * Returns all available net-proxy instances registered in Eureka, deduplicated by public host.
     * The public host is resolved from instance metadata keys in the following order:
     * - "public-host"
     * - "publicHost"
     * - "app.public-host"
     * - "app.publicHost"
     * Falls back to {@link ServiceInstance#getHost()} if none present. The region comes from the
     * "region" metadata key, and the reservable port range from "port-range-min"/"port-range-max",
     * falling back to the globally configured range when an instance does not advertise its own.
     *
     * @return the discovered proxies, in discovery order
     */
    public List<ProxyHost> listProxies() {
        final var instances = discoveryClient.getInstances(SERVICE_ID);
        final Map<String, ProxyHost> byHost = new LinkedHashMap<>();
        for (final ServiceInstance instance : instances) {
            final var md = instance.getMetadata() != null ? instance.getMetadata() : Map.<String, String>of();
            var host = firstNonBlank(
                md.get("public-host"),
                md.get("publicHost"),
                md.get("app.public-host"),
                md.get("app.publicHost")
            );
            if (host == null) {
                host = instance.getHost();
            }
            if (host == null || host.isBlank()) {
                continue;
            }
            final var range = properties.portReservations().range();
            byHost.putIfAbsent(host, new ProxyHost(
                host,
                firstNonBlank(md.get("region"), md.get("app.region")),
                parsePort(md.get("port-range-min"), range.min()),
                parsePort(md.get("port-range-max"), range.max())
            ));
        }
        return new ArrayList<>(byHost.values());
    }

    /**
     * Looks up a single discovered proxy by its public host.
     *
     * @param host the public host to look for
     * @return the matching proxy, or empty when no registered instance advertises that host
     */
    public Optional<ProxyHost> findByHost(final String host) {
        return listProxies().stream()
            .filter(p -> p.host().equals(host))
            .findFirst();
    }

    private int parsePort(final String value, final int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            return fallback;
        }
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
