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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;

@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
    boolean existsBySubdomain(String subdomain);

    @Query(value = "SELECT count(*) > 0 FROM domains WHERE subdomain = :subdomain", nativeQuery = true)
    boolean existsBySubdomainGlobal(@Param("subdomain") String subdomain);

    List<DomainEntity> findAllByAccount(AccountEntity account);

    Optional<DomainEntity> findByAccountAndSubdomain(AccountEntity account, String subdomain);

    Optional<DomainEntity> findByIdAndAccount(UUID id, AccountEntity account);

    Optional<DomainEntity> findBySubdomain(String subdomain);

    Optional<DomainEntity> findByCustomDomain(String customDomain);

    long countByAccount(AccountEntity account);

    long countByAccountAndCustomDomainIsNotNull(AccountEntity account);
}
