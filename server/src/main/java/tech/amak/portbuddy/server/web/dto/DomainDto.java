/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainDto(
    UUID id,
    String subdomain,
    String domain,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
