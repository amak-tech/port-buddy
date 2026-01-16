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

package tech.amak.portbuddy.server.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.service.DomainService;

/**
 * Controller for internal domain operations.
 */
@RestController
@RequestMapping(path = "/api/internal/domains", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalDomainController {

    private final DomainService domainService;

    /**
     * Marks the domain as SSL active.
     *
     * @param domain the custom domain name
     */
    @PostMapping("/ssl-active")
    public void markSslActive(@RequestParam("domain") final String domain) {
        domainService.markSslActive(domain);
    }
}
