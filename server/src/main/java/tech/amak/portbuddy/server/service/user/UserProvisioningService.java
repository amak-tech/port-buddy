/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service.user;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.Role;
import tech.amak.portbuddy.server.db.entity.UserAccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.UserCreatedEvent;
import tech.amak.portbuddy.server.service.DomainService;
import tech.amak.portbuddy.server.service.PortReservationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainService domainService;
    private final PortReservationService portReservationService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetService passwordResetService;

    public record ProvisionedUser(UUID userId, UUID accountId, String accountName, Set<Role> roles) {
    }

    /**
     * Creates a new local user with email and password.
     */
    @Transactional
    public ProvisionedUser createLocalUser(final String email, final String name, final String password) {
        final var normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }

        final var userName = Optional.ofNullable(name)
            .filter(StringUtils::isNotBlank)
            .orElse("Unknown Buddy");

        // Split name
        String lastName = null;
        final var parts = userName.trim().split("\\s+", 2);
        final var firstName = parts[0];
        if (parts.length > 1) {
            lastName = parts[1];
        }

        // Create new account and user
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setName(defaultAccountName(firstName, lastName, normalizedEmail));
        account.setPlan(Plan.PRO);
        accountRepository.save(account);

        final var user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuthProvider("local");
        user.setExternalId(normalizedEmail);
        Optional.ofNullable(password)
            .filter(StringUtils::isNotBlank)
            .map(passwordEncoder::encode)
            .ifPresent(user::setPassword);
        userRepository.save(user);

        final var userAccount = new UserAccountEntity(user, account, determineRoles(true));
        userAccountRepository.save(userAccount);

        // Try to create an initial port reservation and domain for the account by this user
        try {
            domainService.assignRandomDomain(account);
            portReservationService.createReservation(account, user);
        } catch (final Exception ex) {
            log.error("Failed to create initial reservations for account {}: {}", account.getId(), ex.getMessage(), ex);
        }

        String resetPasswordLink = null;

        if (StringUtils.isBlank(password)) {
            resetPasswordLink = passwordResetService.generateResetPasswordLink(user, Duration.ofDays(30));
        }

        // Publish event after user persisted; listener will send email AFTER_COMMIT
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(), account.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), resetPasswordLink
        ));

        return new ProvisionedUser(user.getId(), account.getId(), account.getName(), userAccount.getRoles());
    }

    /**
     * Ensures a user and an owning account exist for the given identity. If the user does not exist,
     * a new account (default plan PRO) and user are created. If the user exists, profile fields are updated.
     *
     * @param provider   the OAuth2 provider registration id (e.g. google, github)
     * @param externalId the unique external identifier from the provider (subject/id)
     * @param email      the email address, can be null
     * @param firstName  optional first name
     * @param lastName   optional last name
     * @param avatarUrl  optional avatar URL
     * @return identifiers of provisioned user and owning account
     */
    @Transactional
    public ProvisionedUser provision(final String provider,
                                     final String externalId,
                                     final String email,
                                     final String firstName,
                                     final String lastName,
                                     final String avatarUrl) {
        final var normalizedEmail = normalizeEmail(email);

        // If user already exists by provider/externalId, allow sign-in even when current OAuth response
        // does not include email (keep stored email). Update profile fields and return.
        final var existing = userRepository.findByAuthProviderAndExternalId(provider, externalId);
        if (existing.isPresent()) {
            final var user = existing.get();
            boolean changed = false;
            if (normalizedEmail != null && !Objects.equals(user.getEmail(), normalizedEmail)) {
                user.setEmail(normalizedEmail);
                changed = true;
            }
            if (!Objects.equals(user.getFirstName(), firstName)) {
                user.setFirstName(firstName);
                changed = true;
            }
            if (!Objects.equals(user.getLastName(), lastName)) {
                user.setLastName(lastName);
                changed = true;
            }
            if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
                user.setAvatarUrl(avatarUrl);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
            final var userAccount = userAccountRepository.findLatestUsedByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User has no accounts"));
            return new ProvisionedUser(user.getId(), userAccount.getAccount().getId(), userAccount.getAccount().getName(), userAccount.getRoles());
        }

        // For new identities we must have a non-null email to create/merge a user
        if (normalizedEmail == null) {
            throw new MissingEmailException("Email is required to provision a user");
        }

        // No user for this provider/external identity. Try to locate an existing user by email
        if (normalizedEmail != null) {
            final var userByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (userByEmail.isPresent()) {
                final var user = userByEmail.get();
                boolean changed = false;
                // Ensure email normalized
                if (!Objects.equals(user.getEmail(), normalizedEmail)) {
                    user.setEmail(normalizedEmail);
                    changed = true;
                }
                if (!Objects.equals(user.getFirstName(), firstName)) {
                    user.setFirstName(firstName);
                    changed = true;
                }
                if (!Objects.equals(user.getLastName(), lastName)) {
                    user.setLastName(lastName);
                    changed = true;
                }
                if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
                    user.setAvatarUrl(avatarUrl);
                    changed = true;
                }
                // Re-link identity to this provider/external to allow future lookups by provider
                if (!Objects.equals(user.getAuthProvider(), provider)) {
                    user.setAuthProvider(provider);
                    changed = true;
                }
                if (!Objects.equals(user.getExternalId(), externalId)) {
                    user.setExternalId(externalId);
                    changed = true;
                }
                if (changed) {
                    userRepository.save(user);
                }
                final var userAccount = userAccountRepository.findLatestUsedByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User has no accounts"));
                return new ProvisionedUser(user.getId(), userAccount.getAccount().getId(), userAccount.getAccount().getName(), userAccount.getRoles());
            }
        }

        // Create new account and user
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setName(defaultAccountName(firstName, lastName, email));
        account.setPlan(Plan.PRO);
        accountRepository.save(account);

        final var user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuthProvider(provider);
        user.setExternalId(externalId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        final var userAccount = new UserAccountEntity(user, account, determineRoles(true));
        userAccountRepository.save(userAccount);

        // Try to create an initial port reservation and domain for the account by this user
        try {
            domainService.assignRandomDomain(account);
            portReservationService.createReservation(account, user);
        } catch (final Exception ignored) {
            // per spec: ignore if no host/port available during account creation
        }

        // Publish event for brand new user
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(), account.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), null
        ));

        return new ProvisionedUser(user.getId(), account.getId(), account.getName(), userAccount.getRoles());
    }

    private static String defaultAccountName(final String firstName, final String lastName, final String email) {
        final var fullName = "%s %s".formatted(firstName, lastName).trim();
        if (!fullName.isEmpty()) {
            return fullName + "’s account";
        }
        if (email != null && !email.isBlank()) {
            final var at = email.indexOf('@');
            final var local = at > 0 ? email.substring(0, at) : email;
            return local + "’s account";
        }
        return "New account";
    }

    private static String normalizeEmail(final String email) {
        if (email == null) {
            return null;
        }
        final var trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private Set<Role> determineRoles(final boolean isAccountOwner) {
        final var roles = new HashSet<Role>();
        if (userRepository.count() == 0) {
            roles.add(Role.ADMIN);
        }
        if (isAccountOwner) {
            roles.add(Role.ACCOUNT_ADMIN);
        } else {
            roles.add(Role.USER);
        }
        return roles;
    }
}
