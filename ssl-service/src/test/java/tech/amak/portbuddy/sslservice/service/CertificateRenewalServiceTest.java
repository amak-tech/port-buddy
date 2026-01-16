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

package tech.amak.portbuddy.sslservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.amak.portbuddy.sslservice.domain.CertificateEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateStatus;
import tech.amak.portbuddy.sslservice.repo.CertificateJobRepository;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;

@ExtendWith(MockitoExtension.class)
class CertificateRenewalServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateJobRepository jobRepository;

    @Mock
    private AcmeCertificateService acmeCertificateService;

    @InjectMocks
    private RenewalScheduler renewalService;

    @Test
    void checkAndRenewCertificates_ShouldTriggerRenewalForExpiringCerts() {
        // Given
        final var cert1 = new CertificateEntity();
        cert1.setDomain("expiring.com");
        cert1.setManaged(true);
        cert1.setExpiresAt(OffsetDateTime.now().plusDays(10));
        cert1.setStatus(CertificateStatus.ACTIVE);

        when(certificateRepository.findAllByManagedTrue()).thenReturn(List.of(cert1));
        when(certificateRepository.findByDomainIgnoreCase("expiring.com")).thenReturn(Optional.of(cert1));

        // When
        renewalService.scheduleRenewals();

        // Then
        verify(acmeCertificateService, times(1)).submitJob(eq("expiring.com"), eq("renewal-scheduler"), eq(true));
    }

    @Test
    void checkAndRenewCertificates_NoExpiringCerts_ShouldDoNothing() {
        // Given
        final var cert1 = new CertificateEntity();
        cert1.setDomain("not-expiring.com");
        cert1.setManaged(true);
        cert1.setExpiresAt(OffsetDateTime.now().plusDays(40));
        cert1.setStatus(CertificateStatus.ACTIVE);

        when(certificateRepository.findAllByManagedTrue()).thenReturn(List.of(cert1));
        when(certificateRepository.findByDomainIgnoreCase("not-expiring.com")).thenReturn(Optional.of(cert1));

        // When
        renewalService.scheduleRenewals();

        // Then
        verify(acmeCertificateService, never()).submitJob(any(), any(), any(Boolean.class));
    }
}
