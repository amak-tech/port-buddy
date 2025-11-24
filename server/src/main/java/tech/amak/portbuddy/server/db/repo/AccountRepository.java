/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.amak.portbuddy.server.db.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
}
