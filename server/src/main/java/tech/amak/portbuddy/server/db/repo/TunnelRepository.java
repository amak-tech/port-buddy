/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
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
        SELECT *
        FROM tunnels t
        WHERE t.account_id = :accountId
        ORDER BY (t.last_heartbeat_at IS NULL), t.last_heartbeat_at DESC, t.created_at DESC""",
        countQuery = "SELECT COUNT(1) FROM tunnels t WHERE t.account_id = :accountId",
        nativeQuery = true)
    Page<TunnelEntity> pageByAccountOrderByLastHeartbeatDescNullsLast(
        @Param("accountId") UUID accountId, Pageable pageable);

    long countByAccountIdAndStatusIn(UUID accountId, List<TunnelStatus> statuses);

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
}
