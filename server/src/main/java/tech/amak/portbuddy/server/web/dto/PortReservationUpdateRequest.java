/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

/**
 * Request body to update a port reservation.
 * Both fields are optional; if a field is null, it will not be changed.
 */
public record PortReservationUpdateRequest(
    String publicHost,
    Integer publicPort
) {
}
