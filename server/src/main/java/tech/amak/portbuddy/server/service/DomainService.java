/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
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

    @Transactional(readOnly = true)
    public List<DomainEntity> getDomains(final AccountEntity account) {
        return domainRepository.findAllByAccount(account);
    }

    @Transactional
    public DomainEntity createDomain(final AccountEntity account) {
        String subdomain;
        int retries = 0;
        do {
            subdomain = generateRandomSubdomain();
            retries++;
        } while (domainRepository.existsBySubdomainGlobal(subdomain) && retries < MAX_RETRIES);

        if (domainRepository.existsBySubdomainGlobal(subdomain)) {
            throw new RuntimeException("Failed to generate unique subdomain after all attempts");
        }

        final var domain = new DomainEntity();
        domain.setId(UUID.randomUUID());
        domain.setSubdomain(subdomain);
        domain.setDomain(properties.gateway().domain());
        domain.setAccount(account);

        log.info("Assigned subdomain {} to account {}", subdomain, account.getId());
        return domainRepository.save(domain);
    }

    @Transactional
    public void assignRandomDomain(final AccountEntity account) {
        createDomain(account);
    }

    @Transactional
    public DomainEntity updateDomain(final UUID id, final AccountEntity account, final String newSubdomain) {
        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        if (isTunnelActive(domain)) {
            throw new RuntimeException("Cannot update domain used by active tunnel");
        }

        if (!domain.getSubdomain().equals(newSubdomain)) {
            if (domainRepository.existsBySubdomainGlobal(newSubdomain)) {
                throw new RuntimeException("Subdomain " + newSubdomain + " is already taken");
            }
            domain.setSubdomain(newSubdomain);
            return domainRepository.save(domain);
        }
        return domain;
    }

    @Transactional
    public void deleteDomain(final UUID id, final AccountEntity account) {
        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        if (isTunnelActive(domain)) {
            throw new RuntimeException("Cannot delete domain used by active tunnel");
        }

        domainRepository.delete(domain);
        log.info("Deleted domain {} for account {}", domain.getSubdomain(), account.getId());
    }

    /**
     * Resolves a domain for the specified account and user, based on the requested domain
     * or available domains associated with the account. If a specific domain is requested,
     * it attempts to return the domain after verifying its availability. Otherwise, it picks
     * an available domain, taking into account resource affinity.
     *
     * @param account         the account entity for which the domain resolution is performed
     * @param requestedDomain the fully-qualified domain name requested by the user, or null if no specific domain is
     *                        requested
     * @param localHost       the local hostname of the user's resource requesting the domain
     * @param localPort       the local port of the user's resource requesting the domain
     * @return the resolved domain entity satisfying the resolution criteria
     * @throws RuntimeException if the requested domain is unavailable or no domains are available for the account
     */
    @Transactional(readOnly = true)
    public DomainEntity resolveDomain(final AccountEntity account,
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
                .filter(domain -> !isTunnelConnected(domain))
                .orElseThrow(() -> new RuntimeException("Domain not found or unavailable: " + requestedDomain));
        }

        // No specific domain requested
        // Filter out CONNECTED domains
        final var availableDomains = domainRepository.findAllByAccount(account).stream()
            .filter(domain -> !isTunnelConnected(domain))
            .toList();

        if (availableDomains.isEmpty()) {
            throw new RuntimeException("No available domains found. Please add a new domain at https://portbuddy.dev/app/domains");
        }

        // Affinity check: Find the last used subdomain for this resource
        final var lastTunnel = tunnelRepository.findUsedTunnel(account.getId(), localHost, localPort);

        if (lastTunnel.isPresent() && lastTunnel.get().getDomain() != null) {
            final var lastDomain = lastTunnel.get().getDomain();
            final var matched = availableDomains.stream()
                .filter(domain -> Objects.equals(domain.getId(), lastDomain.getId()))
                .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        }

        // Pick any
        return availableDomains.getFirst();
    }

    private boolean isTunnelConnected(final DomainEntity domain) {
        return tunnelRepository.existsByDomainAndStatus(domain, TunnelStatus.CONNECTED);
    }

    private boolean isTunnelActive(final DomainEntity domain) {
        return tunnelRepository.existsByDomainAndStatusNot(domain, TunnelStatus.CLOSED);
    }

    private String generateRandomSubdomain() {
        final var animals = new String[] {"falcon", "lynx", "orca", "otter", "swift", "sparrow", "tiger", "puma"};
        final var name = animals[random.nextInt(animals.length)];
        final var num = 1000 + random.nextInt(9000);
        return name + "-" + num;
    }
}
