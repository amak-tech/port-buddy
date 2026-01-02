/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = "accounts")
    @Override
    Optional<UserEntity> findById(UUID id);

    Optional<UserEntity> findByAuthProviderAndExternalId(String authProvider, String externalId);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query("SELECT ua.user FROM UserAccountEntity ua WHERE ua.account = :account")
    List<UserEntity> findAllByAccount(@Param("account") AccountEntity account);
}
