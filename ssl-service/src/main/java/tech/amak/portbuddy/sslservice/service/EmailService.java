/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;

/**
 * Sends notification emails to administrators about DNS setup and results.
 */
public interface EmailService {

    /**
     * Sends DNS TXT record setup instructions to the administrator.
     *
     * @param job certificate job
     * @param records list of maps with keys: name, value
     * @param expiresAt optional expiration of the ACME authorization
     */
    void sendDnsInstructions(CertificateJobEntity job, List<Map<String, String>> records, OffsetDateTime expiresAt);
}
