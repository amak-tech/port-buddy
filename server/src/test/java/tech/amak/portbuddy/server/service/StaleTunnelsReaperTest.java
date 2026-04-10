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
 */

package tech.amak.portbuddy.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tech.amak.portbuddy.server.config.TunnelsProperties;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

class StaleTunnelsReaperTest {

    @Test
    void shouldCloseTunnelsInRegistryWhenReaped() {
        // Given
        final var tunnelRepository = mock(TunnelRepository.class);
        final var tunnelsProperties = new TunnelsProperties();
        tunnelsProperties.setHeartbeatTimeout(Duration.ofMinutes(1));
        final var tunnelRegistry = mock(TunnelRegistry.class);
        final var reaper = new StaleTunnelsReaper(tunnelRepository, tunnelsProperties, tunnelRegistry);

        final var tunnelId = UUID.randomUUID();
        when(tunnelRepository.closeStaleConnected(any(OffsetDateTime.class)))
            .thenReturn(List.of(tunnelId));

        // When
        reaper.closeStaleTunnels();

        // Then
        verify(tunnelRegistry).closeTunnel(tunnelId);
    }
}
