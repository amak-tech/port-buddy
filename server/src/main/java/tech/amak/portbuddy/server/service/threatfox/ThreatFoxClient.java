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

package tech.amak.portbuddy.server.service.threatfox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.config.ThreatFoxProperties;

@FeignClient(
    name = "threatfox-client",
    url = "${threatfox.url}",
    configuration = ThreatFoxClient.Configuration.class
)
@ConditionalOnProperty(value = "threatfox.enabled", havingValue = "true")
public interface ThreatFoxClient {

    @PostMapping("/api/v1/")
    ThreatFoxResponse fetchIoc(@RequestBody final ThreatFoxRequest request);

    @RequiredArgsConstructor
    class Configuration {

        private static final String AUTH_KEY = "Auth-Key";
        private final ThreatFoxProperties properties;

        @Bean
        public RequestInterceptor authorizationHeaderForwarder() {
            return template -> template.header(AUTH_KEY, properties.authKey());
        }
    }
}
