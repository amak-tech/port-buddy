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

package tech.amak.portbuddy.sslservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Jwt jwt,
    Acme acme,
    Storage storage
) {
    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }

    public record Acme(
        String serverUrl,
        String accountKeyPath,
        String contactEmail,
        String accountLocation,
        Retry retry
    ) {
    }

    public record Retry(
        int maxAttempts,
        long initialDelayMs,
        long maxDelayMs,
        double multiplier,
        long jitterMs
    ) {
    }

    public record Storage(
        String certificatesDir
    ) {
    }
}
