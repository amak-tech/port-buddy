/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.DnsInstructionsEmailRequest;
import tech.amak.portbuddy.sslservice.client.ServerClient;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.service.EmailService;

/**
 * Implementation of {@link EmailService} that sends email via server API.
 */
@Service
@Primary
@RequiredArgsConstructor
public class ServerEmailService implements EmailService {

    private final ServerClient serverClient;

    @Override
    public void sendDnsInstructions(
        final CertificateJobEntity job,
        final List<Map<String, String>> records,
        final OffsetDateTime expiresAt
    ) {
        final var request = DnsInstructionsEmailRequest.builder()
            .jobId(job.getId())
            .domain(job.getDomain())
            .contactEmail(job.getContactEmail())
            .records(records)
            .expiresAt(expiresAt)
            .build();

        serverClient.sendDnsInstructions(request);
    }
}
