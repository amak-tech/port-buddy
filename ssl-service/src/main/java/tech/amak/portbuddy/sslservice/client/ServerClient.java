/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import tech.amak.portbuddy.common.dto.DnsInstructionsEmailRequest;

@FeignClient(name = "port-buddy-server")
public interface ServerClient {

    @PostMapping("/api/internal/email/dns-instructions")
    void sendDnsInstructions(@RequestBody final DnsInstructionsEmailRequest request);
}
