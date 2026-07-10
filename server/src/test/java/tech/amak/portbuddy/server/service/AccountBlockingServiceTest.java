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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountBlockingServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TunnelService tunnelService;
    @Mock
    private IpBlacklistService ipBlacklistService;
    @Mock
    private StripeService stripeService;

    @InjectMocks
    private AccountBlockingService service;

    @Test
    void blockAccount_blocksAndSeedsBothBlacklists() {
        final var accountId = UUID.randomUUID();
        final var account = new AccountEntity();
        account.setId(accountId);
        account.setBlocked(false);

        service.blockAccount(account);

        assertTrue(account.isBlocked());
        verify(accountRepository).save(account);
        verify(tunnelService).closeAllTunnels(account);
        verify(ipBlacklistService).blacklistAccountIps(accountId);
    }

    @Test
    void blockAccount_withPaidSubscription_cancelsSubscription() throws StripeException {
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setBlocked(false);
        account.setStripeSubscriptionId("sub_123");
        account.setSubscriptionStatus("active");

        service.blockAccount(account);

        assertTrue(account.isBlocked());
        verify(stripeService).cancelSubscription(account);
        assertEquals("canceled", account.getSubscriptionStatus());
        assertNull(account.getStripeSubscriptionId());
    }

    @Test
    void blockAccount_withoutSubscription_doesNotCallStripe() throws StripeException {
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setBlocked(false);

        service.blockAccount(account);

        assertTrue(account.isBlocked());
        verify(stripeService, never()).cancelSubscription(any(AccountEntity.class));
    }

    @Test
    void blockAccount_whenStripeFails_stillBlocks() throws StripeException {
        final var accountId = UUID.randomUUID();
        final var account = new AccountEntity();
        account.setId(accountId);
        account.setBlocked(false);
        account.setStripeSubscriptionId("sub_123");
        account.setSubscriptionStatus("active");

        doThrow(new ApiConnectionException("stripe down")).when(stripeService).cancelSubscription(account);

        service.blockAccount(account);

        assertTrue(account.isBlocked());
        verify(accountRepository).save(account);
        verify(tunnelService).closeAllTunnels(account);
        // subscription id is retained so it can be retried/reconciled later
        assertEquals("sub_123", account.getStripeSubscriptionId());
    }

    @Test
    void blockAccount_isNoOpWhenAlreadyBlocked() {
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setBlocked(true);

        service.blockAccount(account);

        verify(accountRepository, never()).save(account);
        verify(ipBlacklistService, never()).blacklistAccountIps(account.getId());
    }

    @Test
    void unblockAccount_unblocksAndRemovesBothBlacklists() {
        final var accountId = UUID.randomUUID();
        final var account = new AccountEntity();
        account.setId(accountId);
        account.setBlocked(true);

        service.unblockAccount(account);

        assertFalse(account.isBlocked());
        verify(accountRepository).save(account);
        verify(ipBlacklistService).removeAccountIps(accountId);
    }
}
