/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.service.EmailService;

/**
 * Simple email service that logs messages instead of sending real emails.
 */
@Service
@ConditionalOnMissingBean(EmailService.class)
@Slf4j
public class LogEmailService implements EmailService {

    @Override
    public void sendDnsInstructions(
        final CertificateJobEntity job,
        final List<Map<String, String>> records,
        final OffsetDateTime expiresAt
    ) {
        final var builder = new StringBuilder();
        builder.append("[EMAIL] Action required: Add DNS TXT records for ")
            .append(job.getDomain()).append('\n');
        for (final var rec : records) {
            builder.append("TXT ")
                .append(rec.get("name"))
                .append(" = \"")
                .append(rec.get("value"))
                .append("\"\n");
        }
        if (expiresAt != null) {
            builder.append("Expires at: ").append(expiresAt).append('\n');
        }
        builder.append("Confirm: POST /api/certificates/jobs/")
            .append(job.getId())
            .append("/confirm-dns\n");
        log.info(builder.toString());
    }
}
