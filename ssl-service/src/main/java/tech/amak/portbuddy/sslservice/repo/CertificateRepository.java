/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.amak.portbuddy.sslservice.domain.CertificateEntity;

public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    /**
     * Finds a certificate by domain.
     *
     * @param domain the domain name
     * @return optional entity
     */
    Optional<CertificateEntity> findByDomain(String domain);

    /**
     * Finds a certificate by domain (case-insensitive).
     *
     * @param domain the domain name
     * @return optional entity
     */
    Optional<CertificateEntity> findByDomainIgnoreCase(String domain);

    /**
     * Returns all certificates that are marked as managed by the service.
     *
     * @return list of managed certificates
     */
    List<CertificateEntity> findAllByManagedTrue();
}
