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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.domain.CertificateEntity;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;
import tech.amak.portbuddy.sslservice.service.AcmeCertificateService;

@RestController
@RequestMapping("/internal/api/certificates")
@RequiredArgsConstructor
public class InternalController {

    private final AcmeCertificateService acmeCertificateService;
    private final CertificateRepository certificateRepository;

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
     * Confirms that DNS TXT records were added for the job and continues issuance.
     *
     * @param id job id
     * @return 202 Accepted on success
     */
    @PostMapping("/jobs/{id}/confirm-dns")
    public ResponseEntity<Void> confirmDns(@PathVariable("id") final UUID id) {
        acmeCertificateService.confirmDnsAndContinue(id);
        return ResponseEntity.accepted().build();
    }

}
