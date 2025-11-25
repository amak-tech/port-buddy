/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
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
