/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.domain;

/**
 * Status of SSL certificate lifecycle.
 */
public enum CertificateStatus {
    NEW,
    ACTIVE,
    EXPIRED,
    FAILED
}
