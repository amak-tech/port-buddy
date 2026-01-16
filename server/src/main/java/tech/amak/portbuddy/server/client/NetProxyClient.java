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
                              @RequestParam("type") String protocol,
                              @RequestParam(value = "desiredPort", required = false) Integer desiredPort);

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
