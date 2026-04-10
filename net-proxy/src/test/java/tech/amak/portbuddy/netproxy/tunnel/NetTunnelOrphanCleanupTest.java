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

package tech.amak.portbuddy.netproxy.tunnel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.common.TunnelType;

class NetTunnelOrphanCleanupTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testOrphanedTunnelCleanup() throws IOException, InterruptedException {
        try {
            final var registry = new NetTunnelRegistry(mapper);
            final var tunnelId = UUID.randomUUID();

            // Expose a tunnel - this creates it in byTunnelId
            registry.expose(tunnelId, TunnelType.TCP, 10010);

            assertNotNull(registry.byTunnelId.get(tunnelId), "Tunnel should be present after expose");

            try {
                final var cleanupMethod = NetTunnelRegistry.class.getDeclaredMethod("cleanupOrphanedTunnels");
                cleanupMethod.setAccessible(true);

                // First call immediately - should NOT remove because createdAt is too recent (timeout is 5 mins)
                cleanupMethod.invoke(registry);
                assertNotNull(registry.byTunnelId.get(tunnelId), "Tunnel should still be present immediately");

                // Instead of waiting 5 minutes, we'll set the createdAt timestamp back
                final var tunnel = registry.byTunnelId.get(tunnelId);
                tunnel.setCreatedAt(System.currentTimeMillis() - (6 * 60 * 1000)); // 6 mins ago

                // Second call - should remove
                cleanupMethod.invoke(registry);
                assertNull(registry.byTunnelId.get(tunnelId), "Tunnel should be removed after timeout");
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            // No cleanup needed for static fields
        }
    }
}
