/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.service.AcmeCertificateService;

@RestController
@RequestMapping("/internal/api/certificates/jobs")
@RequiredArgsConstructor
public class InternalJobsController {

    private final AcmeCertificateService acmeCertificateService;

    /**
     * Confirms that DNS TXT records were added for the job and continues issuance.
     *
     * @param id job id
     * @return 202 Accepted on success
     */
    @PostMapping("/{id}/confirm-dns")
    public ResponseEntity<Void> confirmDns(@PathVariable("id") final UUID id) {
        acmeCertificateService.confirmDnsAndContinue(id);
        return ResponseEntity.accepted().build();
    }

}
