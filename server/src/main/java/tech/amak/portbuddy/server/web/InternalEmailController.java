/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.HashMap;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.DnsInstructionsEmailRequest;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.mail.EmailService;

/**
 * Controller for internal email operations.
 */
@RestController
@RequestMapping(path = "/api/internal/email", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class InternalEmailController {

    private final EmailService emailService;
    private final AppProperties properties;

    /**
     * Sends DNS instructions email.
     *
     * @param request the DNS instructions email request
     */
    @PostMapping("/dns-instructions")
    public void sendDnsInstructions(@RequestBody final DnsInstructionsEmailRequest request) {
        log.info("Sending DNS instructions email for job {} and domain {}", request.getJobId(), request.getDomain());
        
        final var model = new HashMap<String, Object>();
        model.put("domain", request.getDomain());
        model.put("records", request.getRecords());
        model.put("expiresAt", request.getExpiresAt());
        model.put("webAppUrl", properties.gateway().url());
        model.put("subject", "Action Required: DNS Setup for " + request.getDomain());

        emailService.sendTemplate(
            request.getContactEmail(),
            "Action Required: DNS Setup for " + request.getDomain(),
            "email/dns-instructions",
            model
        );
    }
}
