/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
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

    public PortBuddySubdomainLoadBalancer(final ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
                                          final String serviceId) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
        this.roundRobin = new RoundRobinLoadBalancer(supplierProvider, serviceId);
        this.webClient = WebClient.builder().build();
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(final Request request) {
        // Extract subdomain from host if present; otherwise delegate.
        final var subdomain = extractSubdomain(request);
        if (!StringUtils.hasText(subdomain)) {
            return roundRobin.choose(request);
        }

        final var supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        return supplier.get().next().flatMap(instances -> {
            if (instances == null || instances.isEmpty()) {
                return Mono.just(new EmptyResponse());
            }

            // Probe all instances concurrently; pick the first that returns 200 OK.
            final var probeTimeout = Duration.ofMillis(500);
            return findOwningInstance(instances, subdomain, probeTimeout)
                .map(DefaultResponse::new)
                .switchIfEmpty(Mono.just(new DefaultResponse(instances.getFirst())));
        });
    }

    private Mono<ServiceInstance> findOwningInstance(final List<ServiceInstance> instances,
                                                     final String subdomain,
                                                     final Duration timeout) {
        return Flux.fromIterable(instances)
            .flatMap(instance -> checkInstance(instance, subdomain, timeout)
                .onErrorResume(ex -> Mono.empty()), instances.size())
            .next();
    }

    private Mono<ServiceInstance> checkInstance(final ServiceInstance instance,
                                                final String subdomain,
                                                final Duration timeout) {
        final var scheme = instance.isSecure() ? "https" : "http";
        final var uri = URI.create("%s://%s:%d/ingress/resolve/%s".formatted(
            scheme, instance.getHost(), instance.getPort(), subdomain));
        return webClient.get()
            .uri(uri)
            .exchangeToMono(resp -> resp.statusCode().is2xxSuccessful() ? Mono.just(instance) : Mono.empty())
            .timeout(timeout)
            .onErrorResume(ex -> Mono.empty());
    }

    private String extractSubdomain(final Request request) {
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
        final var hostOnly = hostHeader.contains(":") ? hostHeader.substring(0, hostHeader.indexOf(':')) : hostHeader;
        final var dotIdx = hostOnly.indexOf('.');
        if (dotIdx <= 0) {
            return null;
        }
        return hostOnly.substring(0, dotIdx);
    }
}
