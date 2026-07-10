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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.amak.portbuddy.server.db.entity.IpBlacklistEntity;
import tech.amak.portbuddy.server.db.repo.IpBlacklistRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.security.IpBlacklistedException;

@ExtendWith(MockitoExtension.class)
class IpBlacklistServiceTest {

    @Mock
    private IpBlacklistRepository ipBlacklistRepository;
    @Mock
    private TunnelRepository tunnelRepository;

    @InjectMocks
    private IpBlacklistService ipBlacklistService;

    @Test
    void blacklistAccountIps_upsertsOnlyNewDistinctIps() {
        final var accountId = UUID.randomUUID();
        when(tunnelRepository.findDistinctClientIpsByAccountId(accountId))
            .thenReturn(List.of("1.1.1.1", "2.2.2.2"));
        when(ipBlacklistRepository.existsByIpAddressAndAccountId("1.1.1.1", accountId)).thenReturn(true);
        when(ipBlacklistRepository.existsByIpAddressAndAccountId("2.2.2.2", accountId)).thenReturn(false);

        ipBlacklistService.blacklistAccountIps(accountId);

        // Only the not-yet-present IP is saved
        verify(ipBlacklistRepository, times(1)).save(any(IpBlacklistEntity.class));
    }

    @Test
    void removeAccountIps_deletesByAccountId() {
        final var accountId = UUID.randomUUID();

        ipBlacklistService.removeAccountIps(accountId);

        verify(ipBlacklistRepository).deleteByAccountId(accountId);
    }

    @Test
    void assertNotBlacklisted_throwsForListedIp() {
        when(ipBlacklistRepository.existsByIpAddress("9.9.9.9")).thenReturn(true);

        assertThrows(IpBlacklistedException.class, () -> ipBlacklistService.assertNotBlacklisted("9.9.9.9"));
    }

    @Test
    void assertNotBlacklisted_passesForUnlistedIp() {
        when(ipBlacklistRepository.existsByIpAddress("8.8.8.8")).thenReturn(false);

        assertDoesNotThrow(() -> ipBlacklistService.assertNotBlacklisted("8.8.8.8"));
    }

    @Test
    void assertNotBlacklisted_passesForNullIp() {
        assertDoesNotThrow(() -> ipBlacklistService.assertNotBlacklisted(null));
        verify(ipBlacklistRepository, never()).existsByIpAddress(any());
    }
}
