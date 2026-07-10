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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.dto.ExposeRequest;
import tech.amak.portbuddy.server.client.NetProxyClient;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.exception.IpBlacklistedException;
import tech.amak.portbuddy.server.exception.SubscriptionException;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

@ExtendWith(MockitoExtension.class)
class TunnelServiceTest {

    @Mock
    private TunnelRepository tunnelRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TunnelRegistry tunnelRegistry;
    @Mock
    private NetProxyClient netProxyClient;
    @Mock
    private IpBlacklistService ipBlacklistService;

    private TunnelService tunnelService;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        final var properties = new AppProperties(
            null, null, null, null, null, null,
            new AppProperties.Subscriptions(
                Duration.ofDays(3),
                Duration.ofHours(1),
                5,
                new AppProperties.Subscriptions.Tunnels(
                    Map.of(Plan.PRO, 1, Plan.TEAM, 10), Map.of(Plan.PRO, 5, Plan.TEAM, 5))),
            null
        );
        tunnelService = new TunnelService(
            tunnelRepository, accountRepository, properties, Optional.empty(), tunnelRegistry, netProxyClient,
            ipBlacklistService);
        account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setPlan(Plan.PRO);
        account.setSubscriptionStatus("active");
    }

    @Test
    void checkTunnelLimit_ActiveSubscription_Success() {
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);
        assertDoesNotThrow(() -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void checkTunnelLimit_InactiveSubscription_ThrowsException() {
        account.setSubscriptionStatus("past_due");
        assertThrows(SubscriptionException.class, () -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void markConnected_InactiveSubscription_ThrowsException() {
        final var tunnelId = UUID.randomUUID();
        final var tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        tunnel.setAccountId(account.getId());

        account.setSubscriptionStatus("canceled");

        when(tunnelRepository.findById(tunnelId)).thenReturn(Optional.of(tunnel));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThrows(SubscriptionException.class, () -> tunnelService.markConnected(tunnelId));
    }

    @Test
    void markConnected_TcpTunnelNotEntitled_ThrowsException() {
        final var tunnelId = UUID.randomUUID();
        final var tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        tunnel.setAccountId(account.getId());
        tunnel.setType(TunnelType.TCP);

        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);

        when(tunnelRepository.findById(tunnelId)).thenReturn(Optional.of(tunnel));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThrows(SubscriptionException.class, () -> tunnelService.markConnected(tunnelId));
    }

    @Test
    void markConnected_TcpTunnelEntitled_Success() {
        final var tunnelId = UUID.randomUUID();
        final var tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        tunnel.setAccountId(account.getId());
        tunnel.setType(TunnelType.TCP);

        account.setPlan(Plan.PRO);
        account.setExtraTunnels(5);

        when(tunnelRepository.findById(tunnelId)).thenReturn(Optional.of(tunnel));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertDoesNotThrow(() -> tunnelService.markConnected(tunnelId));
    }

    @Test
    void heartbeat_TcpTunnelNotEntitled_DoesNotThrow() {
        // Entitlement is enforced only on (re)connect, not on heartbeat, so a currently-live
        // CLI keeps running until it disconnects.
        final var tunnelId = UUID.randomUUID();
        final var tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        tunnel.setAccountId(account.getId());
        tunnel.setType(TunnelType.TCP);

        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);

        when(tunnelRepository.findById(tunnelId)).thenReturn(Optional.of(tunnel));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertDoesNotThrow(() -> tunnelService.heartbeat(tunnelId));
    }

    @Test
    void heartbeat_InactiveSubscription_ThrowsException() {
        final var tunnelId = UUID.randomUUID();
        final var tunnel = new TunnelEntity();
        tunnel.setId(tunnelId);
        tunnel.setAccountId(account.getId());

        account.setSubscriptionStatus("past_due");

        when(tunnelRepository.findById(tunnelId)).thenReturn(Optional.of(tunnel));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThrows(SubscriptionException.class, () -> tunnelService.heartbeat(tunnelId));
    }

    @Test
    void checkTunnelLimit_LimitReached_ThrowsException() {
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(1L);
        account.setExtraTunnels(0);

        final var exception = assertThrows(SubscriptionException.class, () -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));

        assertEquals("Tunnel limit reached for your plan (1). Please upgrade or add more tunnels.",
            exception.getMessage());
    }

    @Test
    void enforceTunnelLimit_ExceedsLimit_ClosesExcessTunnels() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0); // Limit is 1

        final var t1 = new TunnelEntity();
        t1.setId(UUID.randomUUID());
        t1.setStatus(TunnelStatus.CONNECTED);

        final var t2 = new TunnelEntity();
        t2.setId(UUID.randomUUID());
        t2.setStatus(TunnelStatus.CONNECTED);

        when(tunnelRepository.findByAccountIdAndStatusInOrderByLastHeartbeatAtAscCreatedAtAsc(
            eq(account.getId()), any()))
            .thenReturn(List.of(t1, t2));

        tunnelService.enforceTunnelLimit(account);

        assertEquals(TunnelStatus.CLOSED, t1.getStatus());
        // t2 is the second one, so it remains connected if limit is 1
        assertEquals(TunnelStatus.CONNECTED, t2.getStatus());
        verify(tunnelRepository, times(1)).save(t1);
        verify(tunnelRepository, times(0)).save(t2);
    }

    @Test
    void checkTunnelLimit_ProPlanNoSubscription_Success() {
        account.setSubscriptionStatus(null);
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);

        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);
        assertDoesNotThrow(() -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void checkTunnelLimit_ProPlanWithExtraNoSubscription_ThrowsException() {
        account.setSubscriptionStatus(null);
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(1);

        assertThrows(SubscriptionException.class, () -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void checkTunnelLimit_TeamPlanNoSubscription_ThrowsException() {
        account.setSubscriptionStatus(null);
        account.setPlan(Plan.TEAM);

        assertThrows(SubscriptionException.class, () -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void createTunnel_BlacklistedClientIp_ThrowsException() {
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);
        doThrow(new IpBlacklistedException("Client IP is blacklisted"))
            .when(ipBlacklistService).assertNotBlacklisted("6.6.6.6");

        assertThrows(IpBlacklistedException.class, () -> tunnelService.createHttpTunnel(
            account, UUID.randomUUID(), null, createRequest(), "http://abc.pb.dev", new DomainEntity(),
            "6.6.6.6", "curl/7.68.0"));
    }

    @Test
    void isTcpEnabled_ProPlanBelowThreshold_False() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(4);
        assertFalse(tunnelService.isTcpEnabled(account));
    }

    @Test
    void isTcpEnabled_ProPlanAtThreshold_True() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(5);
        assertTrue(tunnelService.isTcpEnabled(account));
    }

    @Test
    void isTcpEnabled_TeamPlanNoExtra_True() {
        account.setPlan(Plan.TEAM);
        account.setExtraTunnels(0);
        assertTrue(tunnelService.isTcpEnabled(account));
    }

    @Test
    void createNetTunnel_TcpFreeTier_ThrowsException() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);

        final var exception = assertThrows(SubscriptionException.class, () -> tunnelService.createNetTunnel(
            account, UUID.randomUUID(), null, createNetRequest(TunnelType.TCP), "127.0.0.1", "curl/7.68.0"));

        assertEquals(
            "TCP tunnels require at least 5 tunnels. Add more tunnels or upgrade to the Team plan.",
            exception.getMessage());
    }

    @Test
    void createNetTunnel_UdpFreeTier_Success() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);

        assertDoesNotThrow(() -> tunnelService.createNetTunnel(
            account, UUID.randomUUID(), null, createNetRequest(TunnelType.UDP), "127.0.0.1", "curl/7.68.0"));
    }

    @Test
    void createNetTunnel_TcpWithEnoughExtraTunnels_Success() {
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(5);
        when(tunnelRepository.countByAccountIdAndStatusIn(any(), any())).thenReturn(0L);

        assertDoesNotThrow(() -> tunnelService.createNetTunnel(
            account, UUID.randomUUID(), null, createNetRequest(TunnelType.TCP), "127.0.0.1", "curl/7.68.0"));
    }

    private ExposeRequest createRequest() {
        return new ExposeRequest(TunnelType.HTTP, "http", "localhost", 8080, null, null, null);
    }

    private ExposeRequest createNetRequest(final TunnelType type) {
        return new ExposeRequest(type, "tcp", "localhost", 5432, null, null, null);
    }
}
