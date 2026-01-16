/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.sslservice.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.repo.CertificateJobRepository;
import tech.amak.portbuddy.sslservice.service.AcmeCertificateService;

@RestController
@RequestMapping("/api/certificates/jobs")
@RequiredArgsConstructor
public class JobsController {

    private final CertificateJobRepository jobRepository;
    private final AcmeCertificateService acmeCertificateService;

    /**
     * Submits a new certificate job.
     *
     * @param domain domain name
     * @param requestedBy who requested the job
     * @param managed whether the certificate should be managed (auto-renewed)
     * @return created job entity
     */
    @PostMapping
    public ResponseEntity<CertificateJobEntity> submitJob(
        @RequestParam("domain") final String domain,
        @RequestParam("requestedBy") final String requestedBy,
        @RequestParam(value = "managed", defaultValue = "false") final boolean managed) {
        return ResponseEntity.ok(acmeCertificateService.submitJob(domain, requestedBy, managed));
    }

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
