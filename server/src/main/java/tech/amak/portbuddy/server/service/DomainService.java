/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.DomainRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class DomainService {

    private static final int MAX_RETRIES = 30;

    private final DomainRepository domainRepository;
    private final TunnelRepository tunnelRepository;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    /**
     * Assigns a randomly generated subdomain to the given account. The subdomain is
     * generated and verified to ensure its uniqueness within the system. If a unique
     * subdomain cannot be generated within the maximum allowed retries, an exception
     * is thrown.
     *
     * @param account the account to which the random subdomain will be assigned
     * @throws RuntimeException if a unique subdomain cannot be generated after the maximum number of retries
     */
    @Transactional
    public void assignRandomDomain(final AccountEntity account) {
        String subdomain;
        int retries = 0;
        do {
            subdomain = generateRandomSubdomain();
            retries++;
        } while (domainRepository.existsBySubdomain(subdomain) && retries < MAX_RETRIES);

        if (domainRepository.existsBySubdomain(subdomain)) {
            throw new RuntimeException("Failed to generate unique subdomain after all attempts");
        }

        final var domain = new DomainEntity();
        domain.setId(UUID.randomUUID());
        domain.setSubdomain(subdomain);
        domain.setDomain(properties.gateway().domain());
        domain.setAccount(account);

        domainRepository.save(domain);
        log.info("Assigned subdomain {} to account {}", subdomain, account.getId());
    }

    /**
     * Resolves a domain for the specified account and user, based on the requested domain
     * or available domains associated with the account. If a specific domain is requested,
     * it attempts to return the domain after verifying its availability. Otherwise, it picks
     * an available domain, taking into account resource affinity.
     *
     * @param account         the account entity for which the domain resolution is performed
     * @param userId          the unique identifier of the user attempting to resolve the domain
     * @param requestedDomain the fully-qualified domain name requested by the user, or null if no specific domain is
     *                        requested
     * @param localHost       the local hostname of the user's resource requesting the domain
     * @param localPort       the local port of the user's resource requesting the domain
     * @return the resolved domain entity satisfying the resolution criteria
     * @throws RuntimeException if the requested domain is unavailable or no domains are available for the account
     */
    @Transactional(readOnly = true)
    public DomainEntity resolveDomain(final AccountEntity account,
                                      final UUID userId,
                                      final String requestedDomain,
                                      final String localHost,
                                      final Integer localPort) {
        if (requestedDomain != null && !requestedDomain.isBlank()) {
            // User requested specific domain
            String targetSubdomain = requestedDomain;
            final var baseDomain = properties.gateway().domain();
            if (requestedDomain.endsWith("." + baseDomain)) {
                targetSubdomain = requestedDomain.substring(0, requestedDomain.length() - baseDomain.length() - 1);
            }

            final var finalSubdomain = targetSubdomain;

            return domainRepository.findByAccountAndSubdomain(account, finalSubdomain)
                .filter(domain -> !isTunnelConnected(domain.getSubdomain()))
                .orElseThrow(() -> new RuntimeException("Domain not found or unavailable: " + requestedDomain));
        }

        // No specific domain requested
        // Filter out CONNECTED domains
        final var availableDomains = domainRepository.findAllByAccount(account).stream()
            .filter(domain -> !isTunnelConnected(domain.getSubdomain()))
            .toList();

        if (availableDomains.isEmpty()) {
            throw new RuntimeException("No available domains found. Please add a new domain at https://portbuddy.dev/app/domains");
        }

        // Affinity check: Find the last used subdomain for this resource
        final var lastTunnel = tunnelRepository.findUsedTunnel(userId, localHost, localPort);

        if (lastTunnel.isPresent()) {
            final var lastSubdomain = lastTunnel.get().getSubdomain();
            final var matched = availableDomains.stream()
                .filter(domain -> domain.getSubdomain().equals(lastSubdomain))
                .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        }

        // Pick any
        return availableDomains.getFirst();
    }

    private boolean isTunnelConnected(String subdomain) {
        return tunnelRepository.existsBySubdomainAndStatus(subdomain, TunnelStatus.CONNECTED);
    }

    private String generateRandomSubdomain() {
        final var animals = new String[] {"falcon", "lynx", "orca", "otter", "swift", "sparrow", "tiger", "puma"};
        final var name = animals[random.nextInt(animals.length)];
        final var num = 1000 + random.nextInt(9000);
        return name + "-" + num;
    }
}
