/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.dto;

public record CertificateResponse(
    String domain,
    String certificatePath,
    String privateKeyPath,
    String chainPath
) {
}
