/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.scheduler;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.domain.CertificateEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateStatus;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;
import tech.amak.portbuddy.sslservice.service.AcmeCertificateService;

/**
 * Periodically scans for certificates that will expire soon and enqueues renewal jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalScheduler {

    private final CertificateRepository certificateRepository;
    private final AcmeCertificateService acmeCertificateService;

    /**
     * Check every hour for certificates expiring in the next 30 days and enqueue renewals.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleRenewals() {
        final var cutoff = OffsetDateTime.now().plusDays(30).truncatedTo(ChronoUnit.MINUTES);
        final List<CertificateEntity> expiring =
            certificateRepository.findByExpiresAtBeforeAndStatus(cutoff, CertificateStatus.ACTIVE);
        for (final var cert : expiring) {
            try {
                acmeCertificateService.submitJob(cert.getDomain(), "system");
            } catch (final Exception e) {
                log.warn("Failed to enqueue renewal for domain={}", cert.getDomain(), e);
            }
        }
    }
}
