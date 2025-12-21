/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.client;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tech.amak.portbuddy.gateway.dto.CertificateResponse;

@Service
@RequiredArgsConstructor
public class SslServiceClient {
    private final WebClient webClient = WebClient.builder().baseUrl("http://ssl-service:8050").build();

    /**
     * Retrieves certificate metadata for a given domain from the ssl-service.
     *
     * @param domain domain name
     * @return certificate response mono
     */
    public Mono<CertificateResponse> getCertificate(final String domain) {
        return webClient.get()
            .uri("/api/certificates/{domain}", domain)
            .retrieve()
            .bodyToMono(CertificateResponse.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.empty());
    }
}
