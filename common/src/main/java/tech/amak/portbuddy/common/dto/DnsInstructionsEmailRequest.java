/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class DnsInstructionsEmailRequest {
    private final UUID jobId;
    private final String domain;
    private final String contactEmail;
    private final List<Map<String, String>> records;
    private final OffsetDateTime expiresAt;
}
