/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.client.SslServiceClient;
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
    private final PasswordEncoder passwordEncoder;
    private final SslServiceClient sslServiceClient;

    @Transactional(readOnly = true)
    public List<DomainEntity> getDomains(final AccountEntity account) {
        return domainRepository.findAllByAccount(account);
    }

    /**
     * Creates a new domain for the given account. A unique subdomain is
     * generated and assigned to the domain entity associated with the account.
     * If a unique subdomain cannot be generated after the maximum number of retries,
     * an exception is thrown.
     *
     * @param account The account entity to which the new domain will be associated.
     * @return The newly created domain entity with its associated subdomain and account.
     * @throws RuntimeException if a unique subdomain cannot be generated within the maximum attempts.
     */
    @Transactional
    public Optional<DomainEntity> createDomain(final AccountEntity account) {
        String subdomain;
        int retries = 0;
        do {
            subdomain = generateRandomSubdomain();
            retries++;
        } while (domainRepository.existsBySubdomainGlobal(subdomain) && retries < MAX_RETRIES);

        if (domainRepository.existsBySubdomainGlobal(subdomain)) {
            log.warn("Failed to generate unique subdomain after all attempts");
            return Optional.empty();
        }

        final var domain = new DomainEntity();
        domain.setId(UUID.randomUUID());
        domain.setSubdomain(subdomain);
        domain.setDomain(properties.gateway().domain());
        domain.setAccount(account);

        log.info("Assigned subdomain {} to account {}", subdomain, account.getId());
        return Optional.of(domainRepository.save(domain));
    }

    @Transactional
    public void assignRandomDomain(final AccountEntity account) {
        createDomain(account);
    }

    /**
     * Updates the subdomain of a specified domain entity associated with a given account.
     * If the domain is currently being used by an active tunnel, the update will not be allowed.
     * Additionally, it ensures that the new subdomain is unique globally before updating.
     *
     * @param id           the unique identifier of the domain to update
     * @param account      the account associated with the domain
     * @param newSubdomain the new subdomain to assign to the domain
     * @return the updated DomainEntity if the operation is successful
     * @throws RuntimeException if the domain is not found for the given id and account
     * @throws RuntimeException if the domain is currently used by an active tunnel
     * @throws RuntimeException if the new subdomain is already taken globally
     */
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

    /**
     * Deletes a domain associated with the specified account.
     * Ensures the domain exists and is not being used by an active tunnel before deletion.
     *
     * @param id      The unique identifier of the domain to be deleted.
     * @param account The account entity associated with the domain to validate ownership.
     * @throws RuntimeException if the domain is not found or is being used by an active tunnel.
     */
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
     * Sets or updates a passcode for the specified domain belonging to the provided account.
     * The passcode will be stored as a hash using the configured {@link PasswordEncoder}.
     *
     * @param id       the unique identifier of the domain
     * @param account  the owner account of the domain
     * @param passcode the new raw passcode value to set
     * @return the updated domain entity
     */
    @Transactional
    public DomainEntity setPasscode(final UUID id, final AccountEntity account, final String passcode) {
        if (passcode == null || passcode.isBlank()) {
            throw new RuntimeException("Passcode must not be blank");
        }
        if (passcode.length() < 4 || passcode.length() > 128) {
            throw new RuntimeException("Passcode length must be between 4 and 128 characters");
        }

        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        final var hash = passwordEncoder.encode(passcode);
        domain.setPasscodeHash(hash);
        return domainRepository.save(domain);
    }

    /**
     * Clears passcode protection for the specified domain belonging to the provided account.
     *
     * @param id      the unique identifier of the domain
     * @param account the owner account of the domain
     */
    @Transactional
    public void clearPasscode(final UUID id, final AccountEntity account) {
        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));
        domain.setPasscodeHash(null);
        domainRepository.save(domain);
    }

    /**
     * Updates the custom domain for the given domain entity.
     *
     * @param id           domain id
     * @param account      account entity
     * @param customDomain custom domain name
     * @return updated domain entity
     */
    @Transactional
    public DomainEntity updateCustomDomain(final UUID id, final AccountEntity account, final String customDomain) {
        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        if (!Objects.equals(domain.getCustomDomain(), customDomain)) {
            domain.setCustomDomain(customDomain);
            domain.setCnameVerified(false);
            return domainRepository.save(domain);
        }
        return domain;
    }

    /**
     * Verifies that the custom domain has a CNAME record pointing to the Port Buddy subdomain.
     * Once verified, it triggers SSL certificate issuance.
     *
     * @param id      domain id
     * @param account account entity
     * @return updated domain entity
     */
    @Transactional
    public DomainEntity verifyCname(final UUID id, final AccountEntity account) {
        final var domain = domainRepository.findByIdAndAccount(id, account)
            .orElseThrow(() -> new RuntimeException("Domain not found"));

        final var customDomain = domain.getCustomDomain();
        if (customDomain == null || customDomain.isBlank()) {
            throw new RuntimeException("Custom domain is not set");
        }

        final var expectedCname = domain.getSubdomain() + "." + domain.getDomain();
        final var isVerified = checkCname(customDomain, expectedCname);

        if (isVerified) {
            domain.setCnameVerified(true);
            domainRepository.save(domain);

            // Trigger SSL issuance
            try {
                final String userEmail = account.getUsers().isEmpty() ? "support@portbuddy.dev" : account.getUsers().get(0).getEmail();
                sslServiceClient.submitJob(customDomain, userEmail, true);
            } catch (final Exception e) {
                log.error("Failed to trigger SSL issuance for {}", customDomain, e);
            }
        } else {
            throw new RuntimeException("CNAME verification failed. Please ensure " + customDomain
                + " points to " + expectedCname);
        }

        return domain;
    }

    private boolean checkCname(final String domain, final String expectedTarget) {
        try {
            final var env = new java.util.Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            final DirContext ictx = new InitialDirContext(env);
            final Attributes attrs = ictx.getAttributes(domain, new String[] {"CNAME"});
            final Attribute cnameAttr = attrs.get("CNAME");

            if (cnameAttr != null) {
                final String cname = (String) cnameAttr.get();
                // Remove trailing dot if present
                final String normalizedCname = cname.endsWith(".") ? cname.substring(0, cname.length() - 1) : cname;
                return normalizedCname.equalsIgnoreCase(expectedTarget);
            }
        } catch (final NamingException e) {
            log.debug("Failed to lookup CNAME for {}", domain, e);
        }
        return false;
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
