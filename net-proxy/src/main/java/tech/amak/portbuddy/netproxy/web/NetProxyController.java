/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.netproxy.web;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.netproxy.config.AppProperties;
import tech.amak.portbuddy.netproxy.tunnel.NetTunnelRegistry;

@RestController
@RequestMapping(path = "/api/net-proxy", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class NetProxyController {

    private final NetTunnelRegistry registry;
    private final AppProperties properties;

    @PostMapping("/expose")
    public ExposeResponse expose(final @RequestParam("tunnelId") UUID tunnelId,
                                 final @RequestParam("protocol") String protocol) throws Exception {
        final var exposedPort = registry.expose(tunnelId, protocol);
        return new ExposeResponse(null, null, properties.publicHost(), exposedPort.getPort(), tunnelId, null);
    }

}
