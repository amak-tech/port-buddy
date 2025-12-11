/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.domain.CertificateEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;
import tech.amak.portbuddy.sslservice.service.AcmeCertificateService;
import tech.amak.portbuddy.sslservice.web.dto.CreateCertificateRequest;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificatesController {

    private final AcmeCertificateService acmeCertificateService;
    private final CertificateRepository certificateRepository;

    /**
     * Creates a new certificate issuance/renewal job for the given domain.
     *
     * @param request        the request containing domain
     * @param authentication current user authentication
     * @return job summary
     */
    @PostMapping
    public ResponseEntity<CertificateJobEntity> createCertificate(
        @Valid @RequestBody final CreateCertificateRequest request,
        final Authentication authentication
    ) {
        final var username = authentication == null ? "system" : authentication.getName();
        final var job = acmeCertificateService.submitJob(request.domain(), username);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * Retrieves certificate metadata for a given domain.
     *
     * @param domain domain name
     * @return 200 with certificate or 404 if not found
     */
    @GetMapping("/{domain}")
    public ResponseEntity<CertificateEntity> getCertificateByDomain(@PathVariable("domain") final String domain) {
        final var normalized = domain.toLowerCase();
        final var entity = certificateRepository.findByDomainIgnoreCase(normalized);
        return entity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Lists certificates with pagination support.
     *
     * @return page of certificates
     */
    @GetMapping
    public Page<CertificateEntity> listCertificates(final Pageable pageable) {
        return certificateRepository.findAll(pageable);
    }

}
