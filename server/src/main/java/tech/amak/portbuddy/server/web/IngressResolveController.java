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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.DomainRepository;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

/**
 * Lightweight, instance-local endpoint for the API Gateway to resolve which server instance
 * currently owns an active tunnel for the given subdomain or custom domain. Returns 200 if
 * this instance has an open tunnel for the subdomain, otherwise 404.
 * This endpoint is intentionally placed under "/ingress/**" which is already permitted in
 * {@link tech.amak.portbuddy.server.security.SecurityConfig} so the gateway can probe it
 * without authentication.
 */
@RestController
@RequestMapping("/ingress")
@RequiredArgsConstructor
public class IngressResolveController {

    private final TunnelRegistry registry;
    private final DomainRepository domainRepository;
    private final AccountRepository accountRepository;

    /**
     * Checks if the given subdomain is owned by an active tunnel.
     *
     * @param subdomain the subdomain to check
     * @return 200 if the subdomain is owned by an active tunnel, 404 otherwise
     */
    @GetMapping("/resolve/{subdomain}")
    public ResponseEntity<Void> resolveOwner(final @PathVariable("subdomain") String subdomain) {
        final var tunnel = registry.getBySubdomain(subdomain);
        if (tunnel != null && tunnel.isOpen() && isSubscriptionActive(tunnel)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Checks if the given custom domain is owned by an active tunnel.
     *
     * @param domain the custom domain to check
     * @return 200 if the custom domain is owned by an active tunnel, 404 otherwise
     */
    @GetMapping("/resolve-custom/{domain}")
    public ResponseEntity<Void> resolveCustomOwner(final @PathVariable("domain") String domain) {
        return domainRepository.findByCustomDomain(domain)
            .map(domainEntity -> {
                final var tunnel = registry.getBySubdomain(domainEntity.getSubdomain());
                if (tunnel != null && tunnel.isOpen() && isSubscriptionActive(tunnel)) {
                    return ResponseEntity.ok().<Void>build();
                }
                return ResponseEntity.notFound().<Void>build();
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isSubscriptionActive(final TunnelRegistry.Tunnel tunnel) {
        return accountRepository.findById(tunnel.accountId())
            .map(account -> {
                final var status = account.getSubscriptionStatus();
                return status == null || "active".equals(status);
            })
            .orElse(false);
    }
}
