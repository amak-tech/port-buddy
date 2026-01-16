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
