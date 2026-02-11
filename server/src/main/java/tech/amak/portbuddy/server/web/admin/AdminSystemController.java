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
 *
 */

package tech.amak.portbuddy.server.web.admin;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.web.admin.dto.SystemStatsResponse;

/**
 * Administrative system endpoints.
 */
@RestController
@RequestMapping(path = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminSystemController {

    private final UserRepository userRepository;
    private final TunnelRepository tunnelRepository;

    /**
     * Returns system-wide statistics for the admin control center.
     * Only users with the ADMIN role can invoke this endpoint.
     *
     * @return a {@link SystemStatsResponse} with total users and active tunnels
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemStatsResponse getSystemStats() {
        final var totalUsers = userRepository.count();
        final var activeTunnels = tunnelRepository.countByStatusIn(List.of(TunnelStatus.CONNECTED));
        return new SystemStatsResponse(totalUsers, activeTunnels);
    }
}
