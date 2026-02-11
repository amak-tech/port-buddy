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

package tech.amak.portbuddy.server.web.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.web.admin.dto.AdminTunnelRow;

@RestController
@RequestMapping(path = "/api/admin/tunnels", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminTunnelController {

    private final TunnelRepository tunnelRepository;

    /**
     * Returns a list of active tunnels for the admin page using a single native SQL query.
     * The list is ordered by last activity (DESC).
     * Accessible only for users with ADMIN role.
     *
     * @param search optional search string to filter tunnels by public address or user
     * @return list of tunnel rows for admin table
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminTunnelRow> listActiveTunnels(
        final @RequestParam(value = "search", required = false) String search) {
        return tunnelRepository.findAdminActiveTunnels(search);
    }

    /**
     * Closes the specified tunnel. Only users with the ADMIN role can invoke this endpoint.
     *
     * @param tunnelId the unique identifier of the tunnel to close
     */
    @PostMapping("/{tunnelId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void closeTunnel(final @PathVariable("tunnelId") UUID tunnelId) {
        final var tunnel = tunnelRepository.findById(tunnelId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tunnel not found"));
        
        tunnel.setStatus(TunnelStatus.CLOSED);
        tunnelRepository.save(tunnel);
    }
}
