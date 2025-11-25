/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.amak.portbuddy.server.db.entity.TunnelEntity;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelEntity, UUID> {

    Optional<TunnelEntity> findByTunnelId(String tunnelId);

    Page<TunnelEntity> findAllByUserId(UUID userId, Pageable pageable);

    @Query(value = "SELECT * FROM tunnels t WHERE t.user_id = :userId "
        + "ORDER BY (t.last_heartbeat_at IS NULL), t.last_heartbeat_at DESC, t.created_at DESC",
        countQuery = "SELECT COUNT(1) FROM tunnels t WHERE t.user_id = :userId",
        nativeQuery = true)
    Page<TunnelEntity> pageByUserOrderByLastHeartbeatDescNullsLast(@Param("userId") UUID userId, Pageable pageable);
}
