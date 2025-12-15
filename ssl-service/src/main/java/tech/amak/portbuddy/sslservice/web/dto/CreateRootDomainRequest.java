/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to onboard a managed root domain for wildcard certificate automation.
 */
public record CreateRootDomainRequest(
    @NotBlank
    @Pattern(
        // Root domain (no wildcard)
        regexp = "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[A-Za-z]{2,}$",
        message = "Invalid root domain"
    )
    String rootDomain,

    Boolean wildcardManaged
) {
}
