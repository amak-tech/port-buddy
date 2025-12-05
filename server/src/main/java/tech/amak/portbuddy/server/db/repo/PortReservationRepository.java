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

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.PortReservationEntity;

public interface PortReservationRepository extends JpaRepository<PortReservationEntity, UUID> {

    List<PortReservationEntity> findAllByAccount(AccountEntity account);

    Optional<PortReservationEntity> findByIdAndAccount(UUID id, AccountEntity account);

    boolean existsByPublicHostAndPublicPort(String publicHost, Integer publicPort);

    long countByPublicHost(String publicHost);

    @Query("select max(pr.publicPort) from PortReservationEntity pr where pr.publicHost = :host")
    Optional<Integer> findMaxPortByHost(@Param("host") String publicHost);

    @Query("select pr from PortReservationEntity pr where pr.account = :account and pr.publicHost = :host and pr.publicPort = :port")
    Optional<PortReservationEntity> findByAccountAndHostPort(@Param("account") AccountEntity account,
                                                             @Param("host") String host,
                                                             @Param("port") Integer port);
}
