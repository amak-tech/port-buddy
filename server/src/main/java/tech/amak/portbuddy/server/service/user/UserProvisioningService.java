/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service.user;

import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.UserCreatedEvent;
import tech.amak.portbuddy.server.service.DomainService;
import tech.amak.portbuddy.server.service.PortReservationService;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainService domainService;
    private final PortReservationService portReservationService;
    private final ApplicationEventPublisher eventPublisher;

    public record ProvisionedUser(UUID userId, UUID accountId) {
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

        // Split name
        String firstName = name;
        String lastName = null;
        if (name != null) {
            final var parts = name.trim().split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1];
            }
        }

        // Create new account and user
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setName(defaultAccountName(firstName, lastName, normalizedEmail));
        account.setPlan("BASIC");
        accountRepository.save(account);
        domainService.assignRandomDomain(account);

        final var user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setAccount(account);
        user.setEmail(normalizedEmail);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuthProvider("local");
        user.setExternalId(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // Try to create an initial port reservation for the account by this user
        try {
            portReservationService.createReservation(account, user);
        } catch (final Exception ignored) {
            // per spec: if no host/port found during new account creation - ignore
        }

        // Publish event after user persisted; listener will send email AFTER_COMMIT
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(), account.getId(), user.getEmail(), user.getFirstName(), user.getLastName()
        ));

        return new ProvisionedUser(user.getId(), account.getId());
    }

    /**
     * Ensures a user and an owning account exist for the given identity. If the user does not exist,
     * a new account (default plan BASIC) and user are created. If the user exists, profile fields are updated.
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
            return new ProvisionedUser(user.getId(), user.getAccount().getId());
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
                return new ProvisionedUser(user.getId(), user.getAccount().getId());
            }
        }

        // Create new account and user
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setName(defaultAccountName(firstName, lastName, email));
        account.setPlan("BASIC");
        accountRepository.save(account);
        domainService.assignRandomDomain(account);

        final var user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setAccount(account);
        user.setEmail(normalizedEmail);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuthProvider(provider);
        user.setExternalId(externalId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        // Try to create an initial port reservation for the account by this user
        try {
            portReservationService.createReservation(account, user);
        } catch (final Exception ignored) {
            // per spec: ignore if no host/port available during account creation
        }

        // Publish event for brand new user
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(), account.getId(), user.getEmail(), user.getFirstName(), user.getLastName()
        ));

        return new ProvisionedUser(user.getId(), account.getId());
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
}
