/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import tech.amak.portbuddy.server.web.admin.dto.AdminAccountRow;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT a FROM AccountEntity a WHERE a.subscriptionStatus <> 'active' AND a.updatedAt < :cutoff")
    List<AccountEntity> findBySubscriptionStatusNotActiveAndUpdatedAtBefore(@Param("cutoff") OffsetDateTime cutoff);

    @Query(value = """
        SELECT a.id AS account_id,
               a.name AS name,
               a.plan AS plan,
               a.extra_tunnels AS extra_tunnels,
               COALESCE(SUM(CASE WHEN t.status = 'CONNECTED' THEN 1 ELSE 0 END), 0) AS active_tunnels,
               a.blocked AS blocked,
               a.created_at AS created_at
        FROM accounts a
        LEFT JOIN tunnels t ON t.account_id = a.id
        GROUP BY a.id, a.name, a.plan, a.extra_tunnels, a.blocked, a.created_at
        ORDER BY active_tunnels DESC, a.created_at DESC
        """, nativeQuery = true)
    List<AdminAccountRow> findAdminAccounts();
}
