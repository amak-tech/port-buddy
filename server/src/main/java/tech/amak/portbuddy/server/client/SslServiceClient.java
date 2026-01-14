/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@FeignClient(
    name = "ssl-service",
    configuration = SslServiceClient.Configuration.class
)
public interface SslServiceClient {

    @PostMapping("/api/certificates/jobs")
    void submitJob(@RequestParam("domain") String domain,
                   @RequestParam("requestedBy") String requestedBy,
                   @RequestParam(value = "managed", defaultValue = "false") boolean managed);

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
