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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.security.ThreatBlockedException;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "threatfox.enabled", havingValue = "true")
public class ThreatFoxService {

    private static final String IOC_TYPE_URL = "url";
    private static final Set<String> IOC_TYPES = Set.of("domain", "ip:port", IOC_TYPE_URL);
    private static final ThreatFoxRequest FETCH_IOC_REQUEST = new ThreatFoxRequest("get_iocs", 1);

    private final ThreatFoxClient client;
    private Set<String> cache = new HashSet<>();

    /**
     * Fetches threat intelligence data and processes it to update the internal cache.
     * This method is scheduled to run periodically, determined by the configuration value
     * specified in the {@code threatfox.fetch-interval} property. It makes a request to
     * retrieve Indicators of Compromise (IOCs) from the ThreatFox API and processes the
     * response to extract and normalize relevant data. The normalized data is stored in a
     * cache, which can later be used to identify and block potential threats.
     * If the fetch operation fails for any reason, an error message is logged to assist
     * in diagnosing the issue.
     * The scheduling configuration specifies the following:
     * - {@code fixedDelayString}: Configured interval between method executions in milliseconds.
     * - {@code initialDelay}: Delay before the first invocation, set to 0.
     * Logs are generated for both successful and failed fetch operations.
     *
     * @throws RuntimeException If an unhandled exception occurs during the fetch or processing.
     */
    @Scheduled(
        fixedDelayString = "${threatfox.fetch-interval}",
        initialDelay = 0
    )
    public void fetchData() {
        try {
            final var threatFoxResponse = client.fetchIoc(FETCH_IOC_REQUEST);
            process(threatFoxResponse);
        } catch (final Exception e) {
            log.error("[Threatfox] Fetch failed: {}", e.getMessage());
        }
    }

    private void process(final ThreatFoxResponse response) {
        if (response == null || response.data() == null) {
            log.warn("[Threatfox] returned empty response");
            return;
        }

        cache = response.data().stream().parallel()
            .filter(ioc -> isRelevantType(ioc.iocType()))
            .map(this::normalize)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        log.info("[Threatfox] cache updated: {} iocs loaded", cache.size());
    }

    private boolean isRelevantType(final String type) {
        return IOC_TYPES.contains(type);
    }

    private String normalize(final ThreatFoxIoc ioc) {
        final var iocValue = Objects.equals(ioc.iocType(), IOC_TYPE_URL)
            ? extractDomain(ioc.ioc())
            : ioc.ioc();
        return normalize(iocValue);
    }

    private String normalize(final String ioc) {
        if (ioc == null || ioc.isBlank()) {
            return null;
        }
        return ioc.toLowerCase().trim();
    }

    private String extractDomain(final String url) {
        final var doubleSlashIndex = url.indexOf("//");
        final var start = doubleSlashIndex == -1 ? 0 : doubleSlashIndex + 2;
        final var lastSlashIndex = url.indexOf("/", start);
        return url.substring(start, lastSlashIndex == -1 ? url.length() : lastSlashIndex);
    }

    private boolean isBlacklisted(final String target) {
        return cache.contains(target);
    }

    /**
     * Checks if the provided host and port combination is blacklisted as a potential threat.
     * <br>
     * The method first normalizes the host by converting it to lowercase and trimming any
     * leading or trailing whitespace. It then verifies if the host alone or the combination
     * of host and port is present in the blacklist cache. If a match is found, a
     * {@code ThreatBlockedException} is thrown to indicate the presence of a threat.
     *
     * @param host The domain or IP address to be checked. Must not be null or empty.
     * @param port The port number associated with the host to be checked.
     *             Should be a valid integer port number.
     * @throws ThreatBlockedException If the host or host-port combination matches an entry in the blacklist.
     */
    public void checkThreat(final String host, final int port) {

        final var normalizeHost = normalize(host);

        if (normalizeHost == null) {
            return;
        }

        if (isBlacklisted(normalizeHost)) {
            log.warn("[Threatfox] Domain {} matches ioc", host);
            throw new ThreatBlockedException("Target domain is blacklisted: " + host);
        }

        final var hostPort = normalizeHost + ":" + port;

        if (isBlacklisted(hostPort)) {
            log.warn("[Threatfox] {} matches ioc", hostPort);
            throw new ThreatBlockedException("Target is blacklisted: " + hostPort);
        }
    }
}
