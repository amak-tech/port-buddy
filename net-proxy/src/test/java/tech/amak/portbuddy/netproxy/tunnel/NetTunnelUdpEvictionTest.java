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
        for (int i = 0; i < 1000; i++) {
            final var id = "remote-" + i;
            tunnel.getUdpRemotes().put(id, new InetSocketAddress("127.0.0.1", 10000 + i));
        }
        
        assertEquals(1000, tunnel.getUdpRemotes().estimatedSize());
        assertNotNull(tunnel.getUdpRemotes().getIfPresent("remote-0"));
        
        // Add one more entry to trigger eviction
        tunnel.getUdpRemotes().put("remote-1000", new InetSocketAddress("127.0.0.1", 11000));
        
        // Caffeine eviction is eventual/asynchronous by default, but for small sizes and immediate check it might work
        // or we can call cleanUp()
        tunnel.getUdpRemotes().cleanUp();
        
        assertEquals(1000, tunnel.getUdpRemotes().estimatedSize());
        assertNotNull(tunnel.getUdpRemotes().getIfPresent("remote-0"),
            "remote-0 should still be present because it was recently accessed");
        assertNull(tunnel.getUdpRemotes().getIfPresent("remote-1"),
            "remote-1 should have been evicted as the eldest entry");
        assertNotNull(tunnel.getUdpRemotes().getIfPresent("remote-1000"));
    }
}
