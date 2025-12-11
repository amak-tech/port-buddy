/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.repo;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateJobStatus;

public interface CertificateJobRepository extends JpaRepository<CertificateJobEntity, UUID> {

    boolean existsByDomainIgnoreCaseAndStatusIn(String domain, Collection<CertificateJobStatus> statuses);
}
