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
    SUCCEEDED,
    FAILED
}
