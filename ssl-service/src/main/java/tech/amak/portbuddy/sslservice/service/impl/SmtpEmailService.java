/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.config.AppProperties;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.service.EmailService;

/**
 * SMTP-based email sender using Spring's {@link JavaMailSender}.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    @Override
    public void sendDnsInstructions(
        final CertificateJobEntity job,
        final List<Map<String, String>> records,
        final OffsetDateTime expiresAt
    ) {
        final var appMail = properties.mail();
        final var recipient = job.getContactEmail() != null && !job.getContactEmail().isBlank()
            ? job.getContactEmail()
            : appMail.defaultTo();

        if (recipient == null || recipient.isBlank()) {
            // No destination configured; quietly return to avoid throwing in critical path
            return;
        }

        final var subject = "Action required: Add DNS TXT records for " + job.getDomain();
        final var body = buildBody(job, records, expiresAt);

        final var msg = new SimpleMailMessage();
        if (appMail.from() != null && !appMail.from().isBlank()) {
            msg.setFrom(appMail.from());
        }
        msg.setTo(recipient);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    private String buildBody(
        final CertificateJobEntity job,
        final List<Map<String, String>> records,
        final OffsetDateTime expiresAt
    ) {
        final var builder = new StringBuilder();
        builder.append("Please add the following DNS TXT records to proceed with SSL issuance.\n\n");
        for (final var rec : records) {
            builder.append("Type: TXT\n")
                .append("Name: ").append(rec.get("name")).append('\n')
                .append("Value: ").append('"').append(rec.get("value")).append('"').append("\n\n");
        }
        if (expiresAt != null) {
            builder.append("Challenge expires at: ")
                .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiresAt))
                .append("\n\n");
        }
        builder.append("After creating the records and waiting for propagation, please confirm here:\n")
            .append("POST /api/certificates/jobs/")
            .append(job.getId())
            .append("/confirm-dns\n");
        return builder.toString();
    }
}
