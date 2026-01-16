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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;

@ExtendWith(MockitoExtension.class)
class PaymentCleanupServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TunnelService tunnelService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private AppProperties.Subscriptions subscriptions;

    private PaymentCleanupService paymentCleanupService;

    @BeforeEach
    void setUp() {
        when(appProperties.subscriptions()).thenReturn(subscriptions);
        paymentCleanupService = new PaymentCleanupService(accountRepository, tunnelService, appProperties);
    }

    @Test
    void cleanupFailedPayments_FindsAccounts_FreezesTunnels() {
        final var gracePeriod = Duration.ofDays(3);
        when(subscriptions.gracePeriod()).thenReturn(gracePeriod);

        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setSubscriptionStatus("past_due");

        when(accountRepository.findBySubscriptionStatusNotActiveAndUpdatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(List.of(account));

        paymentCleanupService.cleanupFailedPayments();

        verify(tunnelService).closeAllTunnels(account);
    }

    @Test
    void cleanupFailedPayments_NoAccounts_DoesNothing() {
        final var gracePeriod = Duration.ofDays(3);
        when(subscriptions.gracePeriod()).thenReturn(gracePeriod);

        when(accountRepository.findBySubscriptionStatusNotActiveAndUpdatedAtBefore(any(OffsetDateTime.class)))
            .thenReturn(List.of());

        paymentCleanupService.cleanupFailedPayments();

        verify(tunnelService, never()).closeAllTunnels(any());
    }
}
