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
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.repo.AccountRepository;

/**
 * Periodically checks for accounts with failed payments or non-active subscriptions
 * and freezes their tunnels after a grace period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCleanupService {

    private final AccountRepository accountRepository;
    private final TunnelService tunnelService;
    private final AppProperties appProperties;

    /**
     * Checks for accounts that need tunnel freezing due to non-active subscription status.
     */
    @Scheduled(
        fixedDelayString =
            "#{@'app-tech.amak.portbuddy.server.config.AppProperties'.subscriptions().checkInterval().toMillis()}",
        initialDelayString =
            "#{@'app-tech.amak.portbuddy.server.config.AppProperties'.subscriptions().checkInterval().toMillis()}"
    )
    @SchedulerLock(name = "paymentCleanupTask", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanupFailedPayments() {
        final var gracePeriod = appProperties.subscriptions().gracePeriod();
        final var cutoff = OffsetDateTime.now().minus(gracePeriod);

        log.debug("Checking for non-active accounts older than {}", cutoff);

        final var accountsToFreeze = accountRepository.findBySubscriptionStatusNotActiveAndUpdatedAtBefore(cutoff);

        if (!accountsToFreeze.isEmpty()) {
            log.info("Found {} accounts to freeze due to non-active subscription status", accountsToFreeze.size());
            for (final var account : accountsToFreeze) {
                log.info("Freezing tunnels for account {} (status={}, updatedAt={})",
                    account.getId(), account.getSubscriptionStatus(), account.getUpdatedAt());
                tunnelService.closeAllTunnels(account);
            }
        }
    }
}
