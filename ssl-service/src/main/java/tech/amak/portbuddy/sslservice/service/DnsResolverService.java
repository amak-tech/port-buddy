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

/**
 * Resolves DNS TXT records and verifies visibility of ACME DNS-01 tokens.
 */
public interface DnsResolverService {

    /**
     * Checks whether a TXT record with the expected value is visible at the given FQDN.
     * Implementations may query multiple public resolvers.
     *
     * @param fqdn fully-qualified domain name of the TXT record
     * @param expectedValue expected TXT value
     * @return true if visible, false otherwise
     */
    boolean isTxtRecordVisible(String fqdn, String expectedValue);
}
