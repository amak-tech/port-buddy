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
