/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response with public exposure details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExposeResponse(
    String source,
    String publicUrl,
    String publicHost,
    Integer publicPort,
    UUID tunnelId,
    String subdomain
) {
}
