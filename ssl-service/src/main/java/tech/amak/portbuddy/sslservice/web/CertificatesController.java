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

import java.util.List;
import java.util.Optional;

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
import tech.amak.portbuddy.sslservice.web.dto.CreateManagedCertificateRequest;

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
        final var job = acmeCertificateService.submitJob(request.domain(), username, false);
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

    /**
     * Onboards or updates a managed certificate entry for a domain. Managed entries are
     * scanned by the renewal scheduler and automatically renewed.
     *
     * @param request request payload
     * @return created/updated certificate metadata
     */
    @PostMapping("/managed")
    public CertificateEntity createManagedCertificate(
        @Valid @RequestBody final CreateManagedCertificateRequest request
    ) {
        final var domain = request.domain().toLowerCase();
        final var certificate = certificateRepository.findByDomainIgnoreCase(domain)
            .orElseGet(() -> CertificateEntity.builder()
                .domain(domain)
                .build());
        certificate.setManaged(true);
        certificate.setVerificationMethod(
            Optional.ofNullable(request.verificationMethod())
                .orElse("MANUAL_DNS01"));
        certificate.setContactEmail(request.contactEmail());
        return certificateRepository.save(certificate);
    }

    /**
     * Lists managed certificate entries.
     *
     * @return page of managed certificates
     */
    @GetMapping("/managed")
    public List<CertificateEntity> listManagedCertificates() {
        return certificateRepository.findAllByManagedTrue();
    }

}
