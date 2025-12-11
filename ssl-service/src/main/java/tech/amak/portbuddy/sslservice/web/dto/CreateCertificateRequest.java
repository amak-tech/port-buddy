/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload to create or renew an SSL certificate for a domain.
 */
public record CreateCertificateRequest(
    @NotBlank
    @Pattern(
        regexp = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[A-Za-z]{2,}$",
        message = "Invalid domain name"
    )
    String domain
) {
}
