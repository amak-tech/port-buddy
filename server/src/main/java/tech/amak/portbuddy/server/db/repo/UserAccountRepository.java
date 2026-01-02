/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.amak.portbuddy.server.db.entity.UserAccountEntity;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UserAccountEntity.UserAccountId> {

    List<UserAccountEntity> findAllByUserId(UUID userId);

    @Query("SELECT ua FROM UserAccountEntity ua WHERE ua.user.id = :userId ORDER BY ua.lastUsedAt DESC LIMIT 1")
    Optional<UserAccountEntity> findLatestUsedByUserId(@Param("userId") UUID userId);

    Optional<UserAccountEntity> findByUserIdAndAccountId(UUID userId, UUID accountId);
}
