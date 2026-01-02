/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.DomainService;
import tech.amak.portbuddy.server.web.dto.DomainDto;
import tech.amak.portbuddy.server.web.dto.SetPasscodeRequest;
import tech.amak.portbuddy.server.web.dto.UpdateCustomDomainRequest;
import tech.amak.portbuddy.server.web.dto.UpdateDomainRequest;

@RestController
@RequestMapping(path = "/api/domains", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DomainsController {

    private final DomainService domainService;
    private final UserRepository userRepository;

    /**
     * Retrieves a list of domains associated with the account of the authenticated user.
     *
     * @param principal the JWT representing the authenticated user
     * @return a list of domain DTOs representing the user's associated domains
     */
    @GetMapping
    public List<DomainDto> list(final @AuthenticationPrincipal Jwt principal) {
        final var account = getAccount(principal);
        return domainService.getDomains(account).stream()
            .map(DomainsController::toDto)
            .toList();
    }

    /**
     * Creates a new domain associated with the authenticated user's account.
     *
     * @param principal the JWT token representing the authenticated user
     * @return the created domain as a DomainDto object
     * @throws RuntimeException if no available domains can be created
     */
    @PostMapping
    public DomainDto create(final @AuthenticationPrincipal Jwt principal) {
        final var account = getAccount(principal);
        return domainService.createDomain(account)
            .map(DomainsController::toDto)
            .orElseThrow(() -> new RuntimeException("No available domains"));
    }

    @PutMapping("/{id}")
    public DomainDto update(final @AuthenticationPrincipal Jwt principal,
                            @PathVariable("id") final UUID id,
                            @RequestBody final UpdateDomainRequest request) {
        final var account = getAccount(principal);
        return toDto(domainService.updateDomain(id, account, request.subdomain()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(final @AuthenticationPrincipal Jwt principal,
                       @PathVariable("id") final UUID id) {
        final var account = getAccount(principal);
        domainService.deleteDomain(id, account);
    }

    /**
     * Sets or updates a passcode for the given domain. The provided passcode will be hashed
     * and stored server-side. Subsequent requests to the public domain will require the passcode.
     *
     * @param principal authenticated user token
     * @param id domain id
     * @param request passcode payload
     * @return updated domain dto
     */
    @PutMapping("/{id}/passcode")
    public DomainDto setPasscode(final @AuthenticationPrincipal Jwt principal,
                                 @PathVariable("id") final UUID id,
                                 @RequestBody final SetPasscodeRequest request) {
        final var account = getAccount(principal);
        return toDto(domainService.setPasscode(id, account, request.passcode()));
    }

    /**
     * Deletes passcode protection for the given domain.
     *
     * @param principal authenticated user token
     * @param id domain id
     */
    @DeleteMapping("/{id}/passcode")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePasscode(final @AuthenticationPrincipal Jwt principal,
                               @PathVariable("id") final UUID id) {
        final var account = getAccount(principal);
        domainService.clearPasscode(id, account);
    }

    /**
     * Updates the custom domain for the given domain.
     *
     * @param principal authenticated user token
     * @param id domain id
     * @param request update payload
     * @return updated domain dto
     */
    @PutMapping("/{id}/custom-domain")
    public DomainDto updateCustomDomain(final @AuthenticationPrincipal Jwt principal,
                                        @PathVariable("id") final UUID id,
                                        @RequestBody final UpdateCustomDomainRequest request) {
        final var account = getAccount(principal);
        return toDto(domainService.updateCustomDomain(id, account, request.customDomain()));
    }

    /**
     * Deletes the custom domain from the given domain.
     *
     * @param principal authenticated user token
     * @param id domain id
     */
    @DeleteMapping("/{id}/custom-domain")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomDomain(final @AuthenticationPrincipal Jwt principal,
                                   @PathVariable("id") final UUID id) {
        final var account = getAccount(principal);
        domainService.deleteCustomDomain(id, account);
    }

    /**
     * Triggers CNAME verification and SSL issuance for the custom domain.
     *
     * @param principal authenticated user token
     * @param id domain id
     * @return updated domain dto
     */
    @PostMapping("/{id}/verify-cname")
    public DomainDto verifyCname(final @AuthenticationPrincipal Jwt principal,
                                 @PathVariable("id") final UUID id) {
        final var account = getAccount(principal);
        return toDto(domainService.verifyCname(id, account));
    }

    private AccountEntity getAccount(final Jwt jwt) {
        final var accountId = tech.amak.portbuddy.server.security.JwtService.resolveAccountId(jwt);
        final var userId = UUID.fromString(jwt.getSubject());
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
            .getAccounts().stream()
            .filter(ua -> ua.getAccount().getId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"))
            .getAccount();
    }

    private static DomainDto toDto(final DomainEntity domain) {
        return new DomainDto(
            domain.getId(),
            domain.getSubdomain(),
            domain.getDomain(),
            domain.getCustomDomain(),
            domain.isCnameVerified(),
            domain.getPasscodeHash() != null && !domain.getPasscodeHash().isBlank(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }
}
