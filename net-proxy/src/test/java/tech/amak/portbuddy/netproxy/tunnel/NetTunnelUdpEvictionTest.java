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

package tech.amak.portbuddy.netproxy.tunnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class NetTunnelUdpEvictionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testUdpRemoteLruEviction() {
        final var registry = new NetTunnelRegistry(mapper);
        final var tunnelId = UUID.randomUUID();
        registry.attachSession(tunnelId, null); // Just create the tunnel object
        
        final var tunnel = registry.byTunnelId.get(tunnelId);
        assertNotNull(tunnel);
        
        // Fill the map to its capacity (1000)
        for (int i = 0; i < 100; i++) {
            final var id = "remote-" + i;
            tunnel.getUdpRemotes().put(id, new InetSocketAddress("127.0.0.1", 10000 + i));
        }
        
        assertEquals(100, tunnel.getUdpRemotes().size());
        assertNotNull(tunnel.getUdpRemotes().get("remote-0"));
        
        // Add one more entry to trigger eviction
        tunnel.getUdpRemotes().put("remote-100", new InetSocketAddress("127.0.0.1", 11000));
        
        assertEquals(100, tunnel.getUdpRemotes().size());
        assertNotNull(tunnel.getUdpRemotes().get("remote-0"),
            "remote-0 should still be present because it was recently accessed");
        assertNull(tunnel.getUdpRemotes().get("remote-1"),
            "remote-1 should have been evicted as the eldest entry");
        assertNotNull(tunnel.getUdpRemotes().get("remote-100"));
    }
}
