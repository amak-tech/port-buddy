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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;

/**
 * Central place for blocking/unblocking an account and the side effects that accompany it
 * (canceling any paid subscription, closing active tunnels, seeding the IP and payload-signature
 * blacklists). Shared by the admin endpoint and by automatic threat responses so both follow the
 * exact same flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBlockingService {

    private final AccountRepository accountRepository;
    private final TunnelService tunnelService;
    private final IpBlacklistService ipBlacklistService;
    private final StripeService stripeService;

    /**
     * Blocks the account (if not already blocked): flips the flag, cancels any active paid
     * subscription, tears down its active tunnels, and adds its tunnels' client IPs and payload
     * signatures to the respective blacklists. Idempotent.
     *
     * @param account the account to block
     */
    @Transactional
    public void blockAccount(final AccountEntity account) {
        if (account.isBlocked()) {
            return;
        }
        account.setBlocked(true);
        cancelActiveSubscription(account);
        accountRepository.save(account);
        tunnelService.closeAllTunnels(account);
        ipBlacklistService.blacklistAccountIps(account.getId());
        log.info("Blocked account {}", account.getId());
    }

    /**
     * Cancels the account's paid Stripe subscription (if any) so a blocked account stops being
     * billed, and optimistically reflects the cancellation locally. A Stripe failure is logged but
     * does not abort the block — blocking is a security action that must still take effect.
     *
     * @param account the account being blocked
     */
    private void cancelActiveSubscription(final AccountEntity account) {
        if (account.getStripeSubscriptionId() == null) {
            return;
        }
        final var subscriptionId = account.getStripeSubscriptionId();
        try {
            stripeService.cancelSubscription(account);
        } catch (final StripeException e) {
            log.error("Failed to cancel Stripe subscription {} while blocking account {}: {}",
                subscriptionId, account.getId(), e.getMessage());
            return;
        }
        account.setSubscriptionStatus("canceled");
        account.setStripeSubscriptionId(null);
        log.info("Canceled subscription {} for blocked account {}", subscriptionId, account.getId());
    }

    /**
     * Unblocks the account (if blocked) and removes the blacklist entries it contributed. Idempotent.
     *
     * @param account the account to unblock
     */
    @Transactional
    public void unblockAccount(final AccountEntity account) {
        if (!account.isBlocked()) {
            return;
        }
        account.setBlocked(false);
        accountRepository.save(account);
        ipBlacklistService.removeAccountIps(account.getId());
        log.info("Unblocked account {}", account.getId());
    }
}
