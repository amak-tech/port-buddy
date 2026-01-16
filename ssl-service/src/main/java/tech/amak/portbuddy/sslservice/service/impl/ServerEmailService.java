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

package tech.amak.portbuddy.sslservice.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.DnsInstructionsEmailRequest;
import tech.amak.portbuddy.sslservice.client.ServerClient;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.service.EmailService;

/**
 * Implementation of {@link EmailService} that sends email via server API.
 */
@Service
@Primary
@RequiredArgsConstructor
public class ServerEmailService implements EmailService {

    private final ServerClient serverClient;

    @Override
    public void sendDnsInstructions(
        final CertificateJobEntity job,
        final List<Map<String, String>> records,
        final OffsetDateTime expiresAt
    ) {
        final var request = DnsInstructionsEmailRequest.builder()
            .jobId(job.getId())
            .domain(job.getDomain())
            .contactEmail(job.getContactEmail())
            .records(records)
            .expiresAt(expiresAt)
            .build();

        serverClient.sendDnsInstructions(request);
    }
}
