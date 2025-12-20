/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ssl-service")
public interface SslServiceClient {

    @PostMapping("/api/certificates/jobs")
    void submitJob(@RequestParam("domain") String domain,
                   @RequestParam("requestedBy") String requestedBy,
                   @RequestParam(value = "managed", defaultValue = "false") boolean managed);
}
