/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.amak.portbuddy.server.db.entity.TunnelEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelEntity, UUID> {

    Optional<TunnelEntity> findByTunnelId(String tunnelId);

    boolean existsBySubdomainAndStatus(String subdomain, TunnelStatus status);

    Optional<TunnelEntity> findFirstByUserIdAndLocalHostAndLocalPortAndSubdomainIsNotNullOrderByCreatedAtDesc(
        UUID userId, String localHost, Integer localPort);

    default Optional<TunnelEntity> findUsedTunnel(UUID userId, String localHost, Integer localPort) {
        return findFirstByUserIdAndLocalHostAndLocalPortAndSubdomainIsNotNullOrderByCreatedAtDesc(
            userId, localHost, localPort);
    }

    Page<TunnelEntity> findAllByUserId(UUID userId, Pageable pageable);

    @Query(value = """
        SELECT *
        FROM tunnels t
        WHERE t.user_id = :userId
        ORDER BY (t.last_heartbeat_at IS NULL), t.last_heartbeat_at DESC, t.created_at DESC""",
        countQuery = "SELECT COUNT(1) FROM tunnels t WHERE t.user_id = :userId",
        nativeQuery = true)
    Page<TunnelEntity> pageByUserOrderByLastHeartbeatDescNullsLast(@Param("userId") UUID userId, Pageable pageable);

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
