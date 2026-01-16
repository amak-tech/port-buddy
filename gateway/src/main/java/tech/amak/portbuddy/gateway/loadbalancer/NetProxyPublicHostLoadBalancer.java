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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * A custom load balancer for the {@code net-proxy} service that selects an instance
 * by matching a request query parameter {@code public-host} against the instance
 * metadata {@code public-host} published to Eureka. If the parameter is missing or
 * no instance matches, the first available instance is selected.
 */
public class NetProxyPublicHostLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final String METADATA_PUBLIC_HOST = "public-host";
    private static final String QUERY_PARAM_PUBLIC_HOST = "public-host";

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final String serviceId;

    /**
     * Constructs load balancer.
     *
     * @param supplierProvider supplier provider
     * @param serviceId        target service id
     */
    public NetProxyPublicHostLoadBalancer(final ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
                                          final String serviceId) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(final Request request) {
        final var supplier = supplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }

        final var requestedPublicHost = extractRequestedPublicHost(request);

        return supplier.get().next().map(instances -> {
            if (instances == null || instances.isEmpty()) {
                return new EmptyResponse();
            }

            if (!StringUtils.hasText(requestedPublicHost)) {
                return new DefaultResponse(instances.getFirst());
            }

            final var matched = findByPublicHost(instances, requestedPublicHost);
            if (matched != null) {
                return new DefaultResponse(matched);
            }

            return new DefaultResponse(instances.getFirst());
        });
    }

    private ServiceInstance findByPublicHost(final List<ServiceInstance> instances, final String publicHost) {
        for (final var instance : instances) {
            final Map<String, String> metadata = instance.getMetadata();
            if (metadata == null) {
                continue;
            }
            final var instanceHost = metadata.get(METADATA_PUBLIC_HOST);
            if (instanceHost != null && instanceHost.equalsIgnoreCase(publicHost)) {
                return instance;
            }
        }
        return null;
    }

    private String extractRequestedPublicHost(final Request request) {
        if (!(request.getContext() instanceof RequestDataContext context)) {
            return null;
        }
        final RequestData data = context.getClientRequest();
        if (data == null) {
            return null;
        }
        final URI url = data.getUrl();
        if (url == null) {
            return null;
        }
        final var query = url.getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        final var pairs = query.split("&");
        for (final var pair : pairs) {
            final var idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            final var name = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            if (!Objects.equals(name, QUERY_PARAM_PUBLIC_HOST)) {
                continue;
            }
            return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}
