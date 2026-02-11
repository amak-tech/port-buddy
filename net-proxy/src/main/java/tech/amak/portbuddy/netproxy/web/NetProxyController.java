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

package tech.amak.portbuddy.netproxy.web;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.netproxy.config.AppProperties;
import tech.amak.portbuddy.netproxy.tunnel.NetTunnelRegistry;

@RestController
@RequestMapping(path = "/api/net-proxy", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NetProxyController {

    private final NetTunnelRegistry registry;
    private final AppProperties properties;

    @PostMapping("/expose")
    public ExposeResponse expose(final @RequestParam("tunnelId") UUID tunnelId,
                                 final @RequestParam("type") TunnelType type,
                                 final @RequestParam(value = "desiredPort", required = false) Integer desiredPort)
        throws Exception {
        final var exposedPort = registry.expose(tunnelId, type, desiredPort);
        return new ExposeResponse(null, null, properties.publicHost(), exposedPort.getPort(), tunnelId, null);
    }

    /**
     * Closes a TCP/UDP tunnel by its ID. Invoked by the server when an account is blocked
     * or when an admin explicitly closes a tunnel.
     *
     * @param tunnelId unique identifier of the tunnel to close
     */
    @PostMapping("/close")
    public void close(final @RequestParam("tunnelId") UUID tunnelId) {
        registry.closeTunnel(tunnelId);
    }
}
