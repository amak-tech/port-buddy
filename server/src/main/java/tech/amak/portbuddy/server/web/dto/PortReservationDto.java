/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PortReservationDto(
    UUID id,
    String publicHost,
    Integer publicPort,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
