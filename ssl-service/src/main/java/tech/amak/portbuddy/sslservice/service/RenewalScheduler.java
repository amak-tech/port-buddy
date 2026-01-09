/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import tech.amak.portbuddy.sslservice.domain.CertificateJobStatus;
import tech.amak.portbuddy.sslservice.domain.CertificateStatus;
import tech.amak.portbuddy.sslservice.repo.CertificateJobRepository;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;

/**
 * Periodically scans managed root domains and creates certificate jobs
 * for wildcard domains when missing or nearing expiry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalScheduler {

    private static final Set<CertificateJobStatus> ACTIVE_JOB_STATUSES = Set.of(
        CertificateJobStatus.PENDING,
        CertificateJobStatus.RUNNING,
        CertificateJobStatus.WAITING_DNS_INSTRUCTIONS,
        CertificateJobStatus.AWAITING_ADMIN_CONFIRMATION,
        CertificateJobStatus.VERIFYING_DNS,
        CertificateJobStatus.FINALIZING
    );
    private static final int RENEW_DAYS_BEFORE = 30;

    private final CertificateRepository certificateRepository;
    private final CertificateJobRepository jobRepository;
    private final AcmeCertificateService acmeCertificateService;

    /**
     * Runs every 5 minutes with 5 seconds initial delay.
     */
    @Scheduled(initialDelay = 5_000, fixedDelay = 300_000)
    @SchedulerLock(name = "RenewalScheduler_scheduleRenewals", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void scheduleRenewals() {
        final var managed = certificateRepository.findAllByManagedTrue();
        if (managed.isEmpty()) {
            return;
        }
        final var cutoff = OffsetDateTime.now().plusDays(RENEW_DAYS_BEFORE);

        for (final var certMeta : managed) {
            final var wildcardDomain = certMeta.getDomain();

            // Skip if there is an active job already
            if (jobRepository.existsByDomainIgnoreCaseAndStatusIn(wildcardDomain, ACTIVE_JOB_STATUSES)) {
                continue;
            }

            final var needsRenewal = certificateRepository.findByDomainIgnoreCase(wildcardDomain)
                .filter(cert -> cert.getStatus() == CertificateStatus.ACTIVE)
                .filter(cert -> cert.getExpiresAt() != null)
                .filter(cert -> cert.getExpiresAt().isAfter(cutoff))
                .isEmpty();

            if (needsRenewal) {
                try {
                    log.info("Scheduling certificate job for {}", wildcardDomain);
                    acmeCertificateService.submitJob(wildcardDomain, "renewal-scheduler", true);
                } catch (final Exception e) {
                    log.warn("Failed to schedule job for {}: {}", wildcardDomain, e.getMessage());
                }
            }
        }
    }
}
