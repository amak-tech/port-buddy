/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

/**
 * Port range for a given tcp-proxy host.
 */
public record PortRangeDto(
    int min,
    int max
) {
}
