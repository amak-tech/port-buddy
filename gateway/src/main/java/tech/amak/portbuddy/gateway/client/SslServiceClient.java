/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.client;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.dto.CertificateResponse;

@Service
@Slf4j
public class SslServiceClient {

    private final WebClient webClient;

    /**
     * Constructs an instance of SslServiceClient with a load-balanced WebClient configured
     * to interact with the ssl-service.
     *
     * @param loadBalancedWebClientBuilder the WebClient.Builder instance used to configure
     *                                     the load-balanced WebClient for communication with
     *                                     the ssl-service
     */
    public SslServiceClient(final WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
            .baseUrl("lb://ssl-service")
            .build();
    }

    /**
     * Retrieves certificate metadata for a given domain from the ssl-service.
     *
     * @param domain domain name
     * @return certificate response mono
     */
    public Mono<CertificateResponse> getCertificate(final String domain) {
        return webClient.get()
            .uri("/internal/api/certificates/{domain}", domain)
            .retrieve()
            .bodyToMono(CertificateResponse.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                log.warn("Failed to retrieve certificate for domain [{}]: {}", domain, e.getMessage());
                return Mono.empty();
            });
    }
}
