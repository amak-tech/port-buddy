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

package tech.amak.portbuddy.gateway.loadbalancer;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Custom load balancer that, for subdomain ingress requests, chooses the server instance
 * that currently holds an active tunnel for the requested subdomain. If no instance confirms
 * ownership, it falls back to the first instance from the list. For all other requests,
 * it delegates to round-robin.
 */
public class PortBuddySubdomainLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final String serviceId;
    private final RoundRobinLoadBalancer roundRobin;
    private final WebClient webClient;

    /**
     * Constructor.
     *
     * @param supplierProvider the service instance supplier provider
     * @param serviceId        service ID
     */
    public PortBuddySubdomainLoadBalancer(final ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
                                          final String serviceId) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
        this.roundRobin = new RoundRobinLoadBalancer(supplierProvider, serviceId);
        this.webClient = WebClient.builder().build();
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(final Request request) {
        // Extract subdomain or custom domain from host if present; otherwise delegate.
        final var host = extractHost(request);
        if (!StringUtils.hasText(host)) {
            return roundRobin.choose(request);
        }

        final var supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        final var isCustomDomain = isCustomDomain(host);
        final var target = isCustomDomain ? host : extractSubdomain(host);

        if (!StringUtils.hasText(target)) {
            return roundRobin.choose(request);
        }

        return supplier.get().next().flatMap(instances -> {
            if (instances == null || instances.isEmpty()) {
                return Mono.just(new EmptyResponse());
            }

            // Probe all instances concurrently; pick the first that returns 200 OK.
            final var probeTimeout = Duration.ofMillis(500);
            return findOwningInstance(instances, target, isCustomDomain, probeTimeout)
                .map(DefaultResponse::new)
                .switchIfEmpty(Mono.just(new DefaultResponse(instances.getFirst())));
        });
    }

    private Mono<ServiceInstance> findOwningInstance(final List<ServiceInstance> instances,
                                                     final String target,
                                                     final boolean isCustomDomain,
                                                     final Duration timeout) {
        return Flux.fromIterable(instances)
            .flatMap(instance -> checkInstance(instance, target, isCustomDomain, timeout)
                .onErrorResume(ex -> Mono.empty()), instances.size())
            .next();
    }

    private Mono<ServiceInstance> checkInstance(final ServiceInstance instance,
                                                final String target,
                                                final boolean isCustomDomain,
                                                final Duration timeout) {
        final var scheme = instance.isSecure() ? "https" : "http";
        final var path = isCustomDomain ? "resolve-custom" : "resolve";
        final var uri = URI.create("%s://%s:%d/ingress/%s/%s".formatted(
            scheme, instance.getHost(), instance.getPort(), path, target));
        return webClient.get()
            .uri(uri)
            .exchangeToMono(resp -> resp.statusCode().is2xxSuccessful() ? Mono.just(instance) : Mono.empty())
            .timeout(timeout)
            .onErrorResume(ex -> Mono.empty());
    }

    private String extractHost(final Request request) {
        if (!(request.getContext() instanceof RequestDataContext context)) {
            return null;
        }
        final var data = context.getClientRequest();
        if (data == null) {
            return null;
        }
        final HttpHeaders headers = data.getHeaders();
        final var hostHeader = headers == null ? null : headers.getFirst(HttpHeaders.HOST);
        if (!StringUtils.hasText(hostHeader)) {
            return null;
        }
        // Strip port if present
        return hostHeader.contains(":") ? hostHeader.substring(0, hostHeader.indexOf(':')) : hostHeader;
    }

    private boolean isCustomDomain(final String host) {
        // Simple logic: if it doesn't end with .portbuddy.dev (or whatever is configured), it's custom.
        // In a real app, this should probably be more robust or use a property.
        return !host.endsWith(".portbuddy.dev") && host.contains(".");
    }

    private String extractSubdomain(final String host) {
        final var dotIdx = host.indexOf('.');
        if (dotIdx <= 0) {
            return null;
        }
        return host.substring(0, dotIdx);
    }
}
