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

package tech.amak.portbuddy.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    int httpPort,
    String domain,
    String url,
    String serverErrorPage,
    Jwt jwt,
    Ssl ssl
) {

    public record Ssl(
        boolean enabled,
        Certificate fallback
    ) {
    }

    public record Certificate(
        boolean enabled,
        Resource keyCertChainFile,
        Resource keyFile
    ) {
    }

    public record Jwt(
        String issuer,
        String jwkSetUri
    ) {
    }

}
