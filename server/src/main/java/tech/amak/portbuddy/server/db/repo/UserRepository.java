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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.web.admin.dto.AdminUserRow;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = "accounts")
    @Override
    Optional<UserEntity> findById(UUID id);

    Optional<UserEntity> findByAuthProviderAndExternalId(String authProvider, String externalId);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query("SELECT ua.user FROM UserAccountEntity ua WHERE ua.account = :account")
    List<UserEntity> findAllByAccount(@Param("account") AccountEntity account);

    @Query(value = """
        SELECT u.id AS id,
               a.id AS account_id,
               CONCAT(u.first_name, ' ', u.last_name) AS name,
               u.email AS email,
               COALESCE(SUM(CASE WHEN t.status = 'CONNECTED' THEN 1 ELSE 0 END), 0) AS active_tunnels,
               a.blocked AS blocked,
               u.created_at AS created_at
        FROM users u
        INNER JOIN user_accounts ua ON ua.user_id = u.id
        INNER JOIN accounts a ON a.id = ua.account_id
        LEFT JOIN tunnels t ON t.account_id = a.id
        WHERE (:search IS NULL
           OR u.email ILIKE CONCAT('%', :search, '%')
           OR u.first_name ILIKE CONCAT('%', :search, '%')
           OR u.last_name ILIKE CONCAT('%', :search, '%')
           OR CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%'))
        GROUP BY u.id, a.id, u.first_name, u.last_name, u.email, a.blocked, u.created_at
        ORDER BY active_tunnels DESC, u.created_at DESC
        """, nativeQuery = true)
    List<AdminUserRow> findAdminUsers(@Param("search") String search);
}
