/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.gateway.loadbalancer.PortBuddySubdomainLoadBalancer;

@Slf4j
public class PortBuddyServerLoadBalancerConfiguration {

    @Bean
    public ReactorServiceInstanceLoadBalancer reactorServiceInstanceLoadBalancer(
        final Environment environment,
        final LoadBalancerClientFactory loadBalancerClientFactory
    ) {
        final var serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        final ObjectProvider<ServiceInstanceListSupplier> provider =
            loadBalancerClientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
        final var loadBalancer = new PortBuddySubdomainLoadBalancer(provider, serviceId);
        log.info("Created PortBuddySubdomainLoadBalancer for service {}", serviceId);
        return loadBalancer;
    }
}
