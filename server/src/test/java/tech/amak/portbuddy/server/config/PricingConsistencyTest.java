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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the one thing that cannot be enforced by a shared module: the server quotes prices and
 * thresholds in messages the CLI prints, while the website quotes them from
 * {@code web/src/config/plans.ts}. Those live in different languages and different build tools, so
 * nothing but a test stops them drifting — and drift here is exactly the bug that had the site
 * describing TCP pricing four different ways.
 *
 * <p>If this fails, do not "fix" one side to match the other without checking Stripe: Stripe is
 * authoritative for what is actually charged, and both files are only mirrors of it.</p>
 */
class PricingConsistencyTest {

    /** Server config, relative to the module directory Surefire runs in. */
    private static final Path SERVER_CONFIG = Path.of("src/main/resources/application.yml");

    /** The website's single source of truth, relative to the same directory. */
    private static final Path WEB_CONFIG = Path.of("../web/src/config/plans.ts");

    private static String serverYaml;
    private static String webConfig;

    @BeforeAll
    static void readConfigs() throws IOException {
        // The web module is absent from some container builds; there is nothing to compare then.
        assumeTrue(Files.exists(WEB_CONFIG), "web/src/config/plans.ts not present, skipping");
        serverYaml = Files.readString(SERVER_CONFIG, StandardCharsets.UTF_8);
        webConfig = Files.readString(WEB_CONFIG, StandardCharsets.UTF_8);
    }

    /**
     * Reads a scalar from the flat {@code key: value} form used throughout application.yml. Good
     * enough here because every key this test cares about is unique in the file.
     */
    private static int yamlInt(final String key) {
        return Integer.parseInt(match(serverYaml, "(?m)^\\s*" + Pattern.quote(key) + ":\\s*(\\d+)\\s*$",
            "%s in %s".formatted(key, SERVER_CONFIG)));
    }

    private static int tsConst(final String name) {
        return Integer.parseInt(match(webConfig, "(?m)^(?:export )?const " + Pattern.quote(name) + " = (\\d+)\\s*$",
            "%s in %s".formatted(name, WEB_CONFIG)));
    }

    private static String match(final String haystack, final String regex, final String what) {
        final var matcher = Pattern.compile(regex).matcher(haystack);
        if (!matcher.find()) {
            throw new AssertionError("Could not find " + what + ". Has it been renamed?");
        }
        return matcher.group(1);
    }

    @Test
    @DisplayName("TCP threshold is the same on the server and on the website")
    void tcpThresholdMatches() {
        assertEquals(yamlInt("tcp-min-extra-tunnels"), tsConst("TCP_MIN_EXTRA_TUNNELS"),
            "app.subscriptions.tcp-min-extra-tunnels and TCP_MIN_EXTRA_TUNNELS disagree");
    }

    @Test
    @DisplayName("Extra-tunnel price is the same on the server and on the website")
    void extraTunnelPriceMatches() {
        assertEquals(yamlInt("extra-tunnel-price"), tsConst("EXTRA_TUNNEL_PRICE"),
            "app.subscriptions.pricing.extra-tunnel-price and EXTRA_TUNNEL_PRICE disagree");
    }

    @Test
    @DisplayName("Team price is the same on the server and on the website")
    void teamPriceMatches() {
        assertEquals(yamlInt("team-price"), tsConst("TEAM_PRICE"),
            "app.subscriptions.pricing.team-price and TEAM_PRICE disagree");
    }

    /**
     * The block size is what makes the advertised TCP price real: the server rejects any change to
     * the extra-tunnel count that is not a multiple of it, so if the site advertised a smaller block
     * the cheapest purchase it describes would be refused at checkout.
     */
    @Test
    @DisplayName("Extra tunnels are sold in the block size the website advertises")
    void extraTunnelBlockMatches() {
        final var proIncrement = Integer.parseInt(match(serverYaml,
            "(?s)increment:.*?\\bpro:\\s*(\\d+)", "app.subscriptions.tunnels.increment.pro"));

        assertEquals(proIncrement, tsConst("EXTRA_TUNNEL_BLOCK"),
            "app.subscriptions.tunnels.increment.pro and EXTRA_TUNNEL_BLOCK disagree");
    }

    /**
     * The threshold must remain a whole number of purchasable blocks, otherwise the cheapest
     * TCP-capable purchase costs more than the derived price the site advertises.
     */
    @Test
    @DisplayName("TCP threshold is a whole number of extra-tunnel blocks")
    void tcpThresholdIsWholeBlocks() {
        assertEquals(0, yamlInt("tcp-min-extra-tunnels") % tsConst("EXTRA_TUNNEL_BLOCK"),
            "TCP threshold is not reachable by buying whole blocks");
    }
}
