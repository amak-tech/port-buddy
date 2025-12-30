/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByAuthProviderAndExternalId(String authProvider, String externalId);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    List<UserEntity> findAllByAccount(AccountEntity account);
}
