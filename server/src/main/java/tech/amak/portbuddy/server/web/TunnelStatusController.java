/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.service.TunnelService;

/**
 * Endpoints to update tunnel connection status from external components (CLI/net-proxy).
 * In HTTP mode the status is tracked automatically via the control WebSocket connected to the server.
 * In TCP mode the control channel is handled by the TCP proxy service, so the CLI reports status here.
 */
@RestController
@RequestMapping(path = "/api/tunnels", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class TunnelStatusController {

    private final TunnelService tunnelService;

    /**
     * Marks a tunnel as connected and updates heartbeat timestamp.
     */
    @PostMapping(path = "/{tunnelId}/connected")
    public void connected(final @PathVariable("tunnelId") UUID tunnelId) {
        log.debug("Status connected for tunnelId={}", tunnelId);
        tunnelService.markConnected(tunnelId);
    }

    /**
     * Updates tunnel heartbeat timestamp.
     */
    @PostMapping(path = "/{tunnelId}/heartbeat")
    public void heartbeat(final @PathVariable("tunnelId") UUID tunnelId) {
        tunnelService.heartbeat(tunnelId);
    }

    /**
     * Marks a tunnel as closed.
     */
    @PostMapping(path = "/{tunnelId}/closed")
    public void closed(final @PathVariable("tunnelId") UUID tunnelId) {
        log.debug("Status closed for tunnelId={}", tunnelId);
        tunnelService.markClosed(tunnelId);
    }
}
