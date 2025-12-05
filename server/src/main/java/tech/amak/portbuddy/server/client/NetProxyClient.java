/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import tech.amak.portbuddy.common.dto.ExposeResponse;

@FeignClient(
    name = "net-proxy",
    configuration = NetProxyClient.Configuration.class
)
public interface NetProxyClient {

    @PostMapping("/api/net-proxy/expose")
    ExposeResponse exposePort(@RequestParam("tunnelId") UUID tunnelId,
                              @RequestParam("protocol") String protocol);

    class Configuration {

        @Bean
        public RequestInterceptor authorizationHeaderForwarder() {
            return template -> {
                final var attributes = RequestContextHolder.getRequestAttributes();
                if (attributes instanceof ServletRequestAttributes requestAttributes) {
                    final var auth = requestAttributes.getRequest().getHeader(AUTHORIZATION);
                    if (auth != null && !auth.isBlank()) {
                        template.header(AUTHORIZATION, auth);
                    }
                }
            };
        }
    }
}
