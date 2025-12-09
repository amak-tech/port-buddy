/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
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
}
