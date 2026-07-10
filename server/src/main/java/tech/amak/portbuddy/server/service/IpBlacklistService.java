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

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.db.entity.IpBlacklistEntity;
import tech.amak.portbuddy.server.db.repo.IpBlacklistRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.security.IpBlacklistedException;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpBlacklistService {

    private static final String BLOCK_REASON = "account blocked";

    private final IpBlacklistRepository ipBlacklistRepository;
    private final TunnelRepository tunnelRepository;

    /**
     * Adds the distinct client IPs of the account's tunnels to the blacklist.
     * Existing (ip, account) entries are skipped so the operation is idempotent.
     *
     * @param accountId the account whose tunnel client IPs should be blacklisted
     */
    @Transactional
    public void blacklistAccountIps(final UUID accountId) {
        final var clientIps = tunnelRepository.findDistinctClientIpsByAccountId(accountId);
        for (final var clientIp : clientIps) {
            if (ipBlacklistRepository.existsByIpAddressAndAccountId(clientIp, accountId)) {
                continue;
            }
            final var entity = new IpBlacklistEntity();
            entity.setId(UUID.randomUUID());
            entity.setIpAddress(clientIp);
            entity.setAccountId(accountId);
            entity.setReason(BLOCK_REASON);
            ipBlacklistRepository.save(entity);
        }
        log.info("Blacklisted {} client IP(s) for account {}", clientIps.size(), accountId);
    }

    /**
     * Removes all blacklist entries contributed by the given account. An IP stays
     * blacklisted while another blocked account still contributed the same IP.
     *
     * @param accountId the account whose blacklist entries should be removed
     */
    @Transactional
    public void removeAccountIps(final UUID accountId) {
        ipBlacklistRepository.deleteByAccountId(accountId);
        log.info("Removed blacklist entries for account {}", accountId);
    }

    /**
     * Throws {@link IpBlacklistedException} if the given client IP is blacklisted.
     * A {@code null} IP is treated as not blacklisted.
     *
     * @param clientIp the client IP address to check
     */
    public void assertNotBlacklisted(final String clientIp) {
        if (clientIp != null && ipBlacklistRepository.existsByIpAddress(clientIp)) {
            log.warn("Rejected request from blacklisted IP {}", clientIp);
            throw new IpBlacklistedException("Client IP is blacklisted");
        }
    }
}
