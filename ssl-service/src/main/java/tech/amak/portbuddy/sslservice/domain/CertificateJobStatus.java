/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.domain;

/**
 * Status of certificate processing job.
 */
public enum CertificateJobStatus {
    PENDING,
    RUNNING,
    WAITING_DNS_INSTRUCTIONS,
    AWAITING_ADMIN_CONFIRMATION,
    VERIFYING_DNS,
    FINALIZING,
    SUCCEEDED,
    FAILED
}
