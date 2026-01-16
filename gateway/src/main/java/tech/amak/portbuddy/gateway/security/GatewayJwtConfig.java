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

package tech.amak.portbuddy.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.gateway.config.AppProperties;

@Configuration
@RequiredArgsConstructor
public class GatewayJwtConfig {

    private final WebClient.Builder loadBalancedWebClientBuilder;
    private final AppProperties properties;

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        final var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwt().jwkSetUri())
            .webClient(loadBalancedWebClientBuilder.build())
            .build();
        final var withIssuer = new JwtIssuerValidator(properties.jwt().issuer());
        final var validator = new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), withIssuer);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
