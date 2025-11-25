/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.amak.portbuddy.server.db.entity.TunnelEntity;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelEntity, UUID> {

    Optional<TunnelEntity> findByTunnelId(String tunnelId);
}
