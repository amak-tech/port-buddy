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

package tech.amak.portbuddy.server.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;

import tech.amak.portbuddy.common.Plan;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Gateway gateway,
    WebSocket webSocket,
    Jwt jwt,
    Mail mail,
    Cli cli,
    PortReservations portReservations,
    Subscriptions subscriptions,
    Stripe stripe
) {
    public record Subscriptions(
        Duration gracePeriod,
        Duration checkInterval,
        Tunnels tunnels
    ) {
        public record Tunnels(Map<Plan, Integer> base, Map<Plan, Integer> increment) {
        }
    }

    public record Gateway(
        String url,
        String domain,
        String subdomainUrlTemplate,
        String notFoundPage,
        String passcodePage
    ) {
        public String subdomainHost() {
            return "." + domain;
        }
    }

    public record WebSocket(
        DataSize maxTextMessageSize,
        DataSize maxBinaryMessageSize,
        Duration sessionIdleTimeout
    ) {
    }

    public record Jwt(
        String issuer,
        Duration ttl,
        Rsa rsa
    ) {
        public record Rsa(
            String currentKeyId,
            List<RsaKey> keys
        ) {
        }

        public record RsaKey(
            String id,
            Resource publicKeyPem,
            Resource privateKeyPem
        ) {
        }
    }

    public record Mail(
        String fromAddress,
        String fromName
    ) {
    }

    public record PortReservations(
        Range range
    ) {
        public record Range(
            int min,
            int max
        ) {
        }
    }

    public record Cli(
        String minVersion
    ) {
    }

    public record Stripe(
        String webhookSecret,
        String apiKey,
        PriceIds priceIds
    ) {
        public record PriceIds(
            String pro,
            String team,
            String extraTunnel
        ) {
        }
    }
}
