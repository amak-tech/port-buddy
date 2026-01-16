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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;

/**
 * Sends notification emails to administrators about DNS setup and results.
 */
public interface EmailService {

    /**
     * Sends DNS TXT record setup instructions to the administrator.
     *
     * @param job certificate job
     * @param records list of maps with keys: name, value
     * @param expiresAt optional expiration of the ACME authorization
     */
    void sendDnsInstructions(CertificateJobEntity job, List<Map<String, String>> records, OffsetDateTime expiresAt);
}
