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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class MainDomainExchangeMatcherTest {

    @ParameterizedTest(name = "domain={0} host={1} -> main={2}")
    @CsvSource({
        // prod-like domain
        "portbuddy.dev, portbuddy.dev, true",
        "portbuddy.dev, PortBuddy.DEV, true",
        "portbuddy.dev, portbuddy.dev:443, true",
        "portbuddy.dev, triel.portbuddy.dev, false",
        "portbuddy.dev, www.portbuddy.dev, false",
        "portbuddy.dev, api.customer-domain.com, false",
        "portbuddy.dev, notportbuddy.dev, false",
        // dev domain carries a port
        "localhost:8443, localhost:8443, true",
        "localhost:8443, localhost, true",
        "localhost:8443, demo.localhost:8443, false",
        "localhost:8443, 127.0.0.1:8443, false",
    })
    void matchesOnlyExactMainDomainHost(final String domain, final String host, final boolean expected) {
        final var matcher = new MainDomainExchangeMatcher(domain);
        final var exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("http://ignored/api/v1/x").header(HttpHeaders.HOST, host));

        assertThat(matcher.isMainDomain(exchange)).isEqualTo(expected);
        assertThat(matcher.matches(exchange).block().isMatch()).isEqualTo(expected);
    }

    @Test
    void fallsBackToRequestUriHostWhenNoHostHeader() {
        final var matcher = new MainDomainExchangeMatcher("portbuddy.dev");

        final var tunnel = MockServerWebExchange.from(MockServerHttpRequest.get("https://triel.portbuddy.dev/x"));
        final var main = MockServerWebExchange.from(MockServerHttpRequest.get("https://portbuddy.dev/x"));

        assertThat(matcher.isMainDomain(tunnel)).isFalse();
        assertThat(matcher.isMainDomain(main)).isTrue();
    }

    @Test
    void treatsUnresolvableHostAsMainDomain() {
        final var matcher = new MainDomainExchangeMatcher("portbuddy.dev");
        final var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/relative/path"));

        assertThat(matcher.isMainDomain(exchange)).isTrue();
    }
}
