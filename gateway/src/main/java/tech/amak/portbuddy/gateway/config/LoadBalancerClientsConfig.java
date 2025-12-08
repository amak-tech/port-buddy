/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * Registers custom load balancer configuration for specific downstream services.
 */
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "port-buddy-server", configuration = PortBuddyServerLoadBalancerConfiguration.class),
    @LoadBalancerClient(name = "net-proxy", configuration = NetProxyLoadBalancerConfiguration.class)
})
public class LoadBalancerClientsConfig {
}
