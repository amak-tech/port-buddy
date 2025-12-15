/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to create or update a managed certificate entry.
 */
public record CreateManagedCertificateRequest(
    @NotBlank
    @Pattern(
        // Allows domains like example.com or *.example.com
        regexp = "^(?:\\*\\.)?(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[A-Za-z]{2,}$",
        message = "Invalid domain name"
    )
    String domain,

    @Email
    String contactEmail,

    // For now supports values like MANUAL_DNS01 or HTTP01. Optional.
    String verificationMethod
) {
}
