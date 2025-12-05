/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common.dto;

import tech.amak.portbuddy.common.TunnelType;

/** Request to expose a local HTTP/TCP/UDP service. */
public record ExposeRequest(
    TunnelType tunnelType,
    String scheme,
    String host,
    int port,
    String domain,
    String portReservation /* optional, format: host:port for TCP/UDP */
) {}
