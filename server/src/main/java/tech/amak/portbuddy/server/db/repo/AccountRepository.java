/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.amak.portbuddy.server.db.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT a FROM AccountEntity a WHERE a.subscriptionStatus <> 'active' AND a.updatedAt < :cutoff")
    List<AccountEntity> findBySubscriptionStatusNotActiveAndUpdatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
