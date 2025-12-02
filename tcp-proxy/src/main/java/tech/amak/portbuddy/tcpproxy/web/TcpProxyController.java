/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.tcpproxy.web;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.tcpproxy.config.AppProperties;
import tech.amak.portbuddy.tcpproxy.tunnel.TcpTunnelRegistry;

@RestController
@RequestMapping(path = "/api/tcp-proxy", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TcpProxyController {

    private final TcpTunnelRegistry registry;
    private final AppProperties properties;

    @PostMapping("/expose")
    public ExposeResponse expose(final @RequestParam("tunnelId") UUID tunnelId) throws Exception {
        final var exposedPort = registry.expose(tunnelId);
        return new ExposeResponse(null, null, properties.publicHost(), exposedPort.getPort(), tunnelId, null);
    }

}
