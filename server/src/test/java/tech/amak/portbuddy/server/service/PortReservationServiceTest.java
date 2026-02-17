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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.PortReservationRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@ExtendWith(MockitoExtension.class)
class PortReservationServiceTest {

    @Mock
    private PortReservationRepository repository;
    @Mock
    private ProxyDiscoveryService proxyDiscoveryService;
    @Mock
    private TunnelRepository tunnelRepository;
    @Mock
    private AppProperties properties;

    private PortReservationService service;
    private AccountEntity account;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new PortReservationService(repository, proxyDiscoveryService, tunnelRepository, properties);
        account = new AccountEntity();
        account.setId(UUID.randomUUID());
        user = new UserEntity();
        user.setId(UUID.randomUUID());
    }

    @Test
    void resolveForNetExpose_ExplicitHostPort_Success() {
        final String host = "proxy-1.portbuddy.dev";
        final int port = 12345;
        final String explicit = host + ":" + port;
        final var reservation = new PortReservationEntity();
        reservation.setPublicHost(host);
        reservation.setPublicPort(port);

        when(repository.findByAccountAndPublicHostAndPublicPort(account, host, port))
            .thenReturn(Optional.of(reservation));
        when(tunnelRepository.existsByPortReservationAndStatusNot(reservation, TunnelStatus.CLOSED))
            .thenReturn(false);

        final var result = service.resolveForNetExpose(account, user, "localhost", 8080, explicit);

        assertEquals(reservation, result);
    }

    @Test
    void resolveForNetExpose_ExplicitName_Success() {
        final String name = "my-reservation";
        final var reservation = new PortReservationEntity();
        reservation.setName(name);

        when(repository.findByAccountAndNameIgnoreCase(account, name))
            .thenReturn(Optional.of(reservation));
        when(tunnelRepository.existsByPortReservationAndStatusNot(reservation, TunnelStatus.CLOSED))
            .thenReturn(false);

        final var result = service.resolveForNetExpose(account, user, "localhost", 8080, name);

        assertEquals(reservation, result);
    }

    @Test
    void resolveForNetExpose_ExplicitPortOnly_MultipleFound_TakesFirst() {
        final int port = 12345;
        final String explicit = String.valueOf(port);

        final var reservation1 = new PortReservationEntity();
        reservation1.setPublicHost("proxy-1.portbuddy.dev");
        reservation1.setPublicPort(port);

        final var reservation2 = new PortReservationEntity();
        reservation2.setPublicHost("proxy-2.portbuddy.dev");
        reservation2.setPublicPort(port);

        when(repository.findAllByAccountAndPublicPort(account, port))
            .thenReturn(List.of(reservation1, reservation2));
        when(tunnelRepository.existsByPortReservationAndStatusNot(reservation1, TunnelStatus.CLOSED))
            .thenReturn(false);

        final var result = service.resolveForNetExpose(account, user, "localhost", 8080, explicit);

        assertEquals(reservation1, result);
    }

    @Test
    void resolveForNetExpose_ExplicitPortOnly_NotFound_ThrowsException() {
        final int port = 12345;
        final String explicit = String.valueOf(port);

        when(repository.findAllByAccountAndPublicPort(account, port))
            .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
            service.resolveForNetExpose(account, user, "localhost", 8080, explicit));
    }

    @Test
    void updateReservation_DuplicateName_ThrowsException() {
        final UUID id = UUID.randomUUID();
        final String name = "duplicate-name";
        final var existing = new PortReservationEntity();
        existing.setName("old-name");

        when(repository.findByIdAndAccount(id, account)).thenReturn(Optional.of(existing));
        when(repository.existsByAccountAndName(account, name)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            service.updateReservation(account, id, null, null, name));
    }

    @Test
    void updateReservation_SameName_Success() {
        final UUID id = UUID.randomUUID();
        final String name = "same-name";
        final var existing = new PortReservationEntity();
        existing.setName(name);

        when(repository.findByIdAndAccount(id, account)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        final var result = service.updateReservation(account, id, null, null, name);
        assertEquals(name, result.getName());
    }
}
