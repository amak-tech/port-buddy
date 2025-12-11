/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.repo.CertificateJobRepository;

@RestController
@RequestMapping("/api/certificates/jobs")
@RequiredArgsConstructor
public class JobsController {

    private final CertificateJobRepository jobRepository;

    /**
     * Returns job by id.
     *
     * @param id job id
     * @return job entity or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<CertificateJobEntity> getJob(@PathVariable("id") final UUID id) {
        final var job = jobRepository.findById(id);
        return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Lists jobs with pagination.
     *
     * @return page of jobs
     */
    @GetMapping
    public Page<CertificateJobEntity> listJobs(final Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

}
