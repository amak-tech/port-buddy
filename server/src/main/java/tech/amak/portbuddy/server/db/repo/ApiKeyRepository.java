/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.amak.portbuddy.server.db.entity.ApiKeyEntity;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    List<ApiKeyEntity> findAllByUserId(UUID userId);

    List<ApiKeyEntity> findAllByAccountId(UUID accountId);

    Optional<ApiKeyEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ApiKeyEntity> findByIdAndAccountId(UUID id, UUID accountId);

    Optional<ApiKeyEntity> findByTokenHashAndRevokedFalse(String tokenHash);
}
