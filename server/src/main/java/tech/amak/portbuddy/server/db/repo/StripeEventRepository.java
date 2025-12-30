/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.amak.portbuddy.server.db.entity.StripeEventEntity;

public interface StripeEventRepository extends JpaRepository<StripeEventEntity, String> {
}
