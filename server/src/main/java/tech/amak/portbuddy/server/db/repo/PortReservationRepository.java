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

package tech.amak.portbuddy.server.db.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;

public interface PortReservationRepository extends JpaRepository<PortReservationEntity, UUID> {

    List<PortReservationEntity> findAllByAccount(AccountEntity account);

    Optional<PortReservationEntity> findByIdAndAccount(UUID id, AccountEntity account);

    boolean existsByPublicHostAndPublicPort(String publicHost, Integer publicPort);

    long countByPublicHost(String publicHost);

    @Query("select max(pr.publicPort) from PortReservationEntity pr where pr.publicHost = :host")
    Optional<Integer> findMaxPortByHost(@Param("host") String publicHost);

    Optional<PortReservationEntity> findByAccountAndPublicHostAndPublicPort(AccountEntity account,
                                                                            String host,
                                                                            Integer port);

    List<PortReservationEntity> findAllByAccountAndPublicPort(AccountEntity account, Integer port);

    boolean existsByAccountAndName(AccountEntity account, String name);

    Optional<PortReservationEntity> findByAccountAndNameIgnoreCase(AccountEntity account, String name);

    /**
     * Finds the minimal free public port for the specified host within the provided inclusive range
     * using a single SQL query. Only non-deleted reservations are considered busy.
     */
    @Query(value = """
        select gs.port
        from generate_series(:min, :max) as gs(port)
        left join port_reservations pr
               on pr.public_host = :host
              and pr.public_port = gs.port
              and pr.deleted = false
        where pr.public_port is null
        order by gs.port
        limit 1
        """, nativeQuery = true)
    Optional<Integer> findMinimalFreePort(@Param("host") String host,
                                          @Param("min") int min,
                                          @Param("max") int max);
}
