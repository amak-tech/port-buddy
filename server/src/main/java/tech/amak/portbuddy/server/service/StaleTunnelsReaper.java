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

package tech.amak.portbuddy.server.service;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import tech.amak.portbuddy.server.config.TunnelsProperties;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

/**
 * Periodically closes tunnels that stopped sending heartbeats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaleTunnelsReaper {

    private final TunnelRepository tunnelRepository;
    private final TunnelsProperties tunnelsProperties;

    /**
     * Monitors stale tunnels and closes them.
     */
    @Scheduled(
        fixedDelayString = "#{@tunnelsProperties.checkInterval.toMillis()}",
        initialDelayString = "#{@tunnelsProperties.checkInterval.toMillis()}"
    )
    @SchedulerLock(name = "staleTunnelsReaper", lockAtMostFor = "PT4M", lockAtLeastFor = "PT3S")
    @Transactional
    public void closeStaleTunnels() {
        final var timeout = tunnelsProperties.getHeartbeatTimeout();
        final var cutoff = OffsetDateTime.now().minus(timeout);
        final var updated = tunnelRepository.closeStaleConnected(cutoff);
        if (updated > 0) {
            log.info("Closed {} stale tunnels (cutoff={})", updated, cutoff);
        } else {
            log.debug("No stale tunnels found (cutoff={})", cutoff);
        }
    }
}
