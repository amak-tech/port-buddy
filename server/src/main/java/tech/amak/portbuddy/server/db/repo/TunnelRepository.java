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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;
import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.web.admin.dto.AdminTunnelRow;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelEntity, UUID> {

    boolean existsByDomainAndStatus(DomainEntity domain, TunnelStatus status);

    boolean existsByDomainAndStatusNot(DomainEntity domain, TunnelStatus status);

    Optional<TunnelEntity> findFirstByAccountIdAndLocalHostAndLocalPortAndDomainIsNotNullOrderByCreatedAtDesc(
        UUID accountId, String localHost, Integer localPort);

    default Optional<TunnelEntity> findUsedTunnel(final UUID accountId,
                                                  final String localHost,
                                                  final Integer localPort) {
        return findFirstByAccountIdAndLocalHostAndLocalPortAndDomainIsNotNullOrderByCreatedAtDesc(
            accountId, localHost, localPort);
    }

    Page<TunnelEntity> findAllByAccountId(UUID accountId, Pageable pageable);

    boolean existsByPortReservationAndStatusNot(PortReservationEntity portReservation, TunnelStatus status);

    Optional<TunnelEntity> findFirstByAccountIdAndLocalHostAndLocalPortAndPortReservationIsNotNullOrderByCreatedAtDesc(
        UUID accountId, String localHost, Integer localPort);

    @Query(value = """
        SELECT t
        FROM TunnelEntity t
        LEFT JOIN FETCH t.portReservation
        LEFT JOIN FETCH t.domain
        WHERE t.accountId = :accountId
        ORDER BY t.lastHeartbeatAt DESC NULLS LAST, t.createdAt DESC""")
    Page<TunnelEntity> pageByAccountOrderByLastHeartbeatDescNullsLast(
        @Param("accountId") UUID accountId, Pageable pageable);

    long countByAccountIdAndStatusIn(UUID accountId, List<TunnelStatus> statuses);

    List<TunnelEntity> findByAccountIdAndStatusInOrderByLastHeartbeatAtAscCreatedAtAsc(
        UUID accountId, List<TunnelStatus> statuses);

    long countByStatusIn(List<TunnelStatus> statuses);

    /**
     * Closes tunnels that are in CONNECTED status but have stale or missing heartbeat.
     * Uses native SQL to also update the updated_at timestamp.
     *
     * @param cutoff heartbeats older than this timestamp are considered stale
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE tunnels SET status = 'CLOSED', updated_at = NOW()
        WHERE created_at < :cutoff
                  AND status <> 'CLOSED'
                  AND (last_heartbeat_at IS NULL OR last_heartbeat_at < :cutoff)""",
        nativeQuery = true)
    int closeStaleConnected(@Param("cutoff") final OffsetDateTime cutoff);

    @Query(value = """
        SELECT t.id AS id,
               t.type AS type,
               CONCAT(t.local_host, ':', t.local_port) AS local_address,
               COALESCE(t.public_url, CONCAT(t.public_host, ':', t.public_port)) AS public_address,
               t.last_heartbeat_at AS last_activity,
               CONCAT(u.first_name, ' ', u.last_name) AS user_name,
               t.user_id AS user_id,
               t.account_id AS account_id
        FROM tunnels t
        LEFT JOIN users u ON u.id = t.user_id
        WHERE t.status = 'CONNECTED'
          AND (:search IS NULL 
               OR t.public_url ILIKE CONCAT('%', :search, '%') 
               OR t.public_host ILIKE CONCAT('%', :search, '%')
               OR u.email ILIKE CONCAT('%', :search, '%')
               OR u.first_name ILIKE CONCAT('%', :search, '%')
               OR u.last_name ILIKE CONCAT('%', :search, '%'))
        ORDER BY t.last_heartbeat_at DESC NULLS LAST, t.created_at DESC
        """, nativeQuery = true)
    List<AdminTunnelRow> findAdminActiveTunnels(@Param("search") String search);
}
