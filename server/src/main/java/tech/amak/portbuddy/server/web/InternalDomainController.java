/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.service.DomainService;

/**
 * Controller for internal domain operations.
 */
@RestController
@RequestMapping(path = "/api/internal/domains", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalDomainController {

    private final DomainService domainService;

    /**
     * Marks the domain as SSL active.
     *
     * @param domain the custom domain name
     */
    @PostMapping("/ssl-active")
    public void markSslActive(@RequestParam("domain") final String domain) {
        domainService.markSslActive(domain);
    }
}
