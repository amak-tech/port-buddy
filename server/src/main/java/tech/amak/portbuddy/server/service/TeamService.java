/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.InvitationEntity;
import tech.amak.portbuddy.server.db.entity.Role;
import tech.amak.portbuddy.server.db.entity.UserAccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.InvitationRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.mail.EmailService;
import tech.amak.portbuddy.server.service.user.UserProvisioningService;

/**
 * Service for managing team members and invitations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;
    private final AppProperties properties;

    /**
     * Returns all members of the team.
     *
     * @param account the account to get members for.
     * @return list of team members.
     */
    public List<UserEntity> getMembers(final AccountEntity account) {
        return userRepository.findAllByAccount(account);
    }

    /**
     * Returns all pending invitations for the team.
     *
     * @param account the account to get invitations for.
     * @return list of pending invitations.
     */
    public List<InvitationEntity> getPendingInvitations(final AccountEntity account) {
        return invitationRepository.findAllByAccountAndAcceptedAtIsNull(account);
    }

    /**
     * Invites a new member to the team.
     *
     * @param account   the account to invite to.
     * @param invitedBy the user who is sending the invitation.
     * @param email     the email of the invited person.
     * @return the created invitation.
     */
    @Transactional
    public InvitationEntity inviteMember(final AccountEntity account,
                                         final UserEntity invitedBy,
                                         final String email) {
        if (account.getPlan() != Plan.TEAM) {
            throw new IllegalStateException("Invitations are only available for Team plan.");
        }

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (userAccountRepository.findByUserIdAndAccountId(user.getId(), account.getId()).isPresent()) {
                throw new IllegalStateException("User is already a member of this team.");
            }
        });

        invitationRepository.findByAccountAndEmailAndAcceptedAtIsNull(account, email).ifPresent(inv -> {
            throw new IllegalStateException("An invitation has already been sent to this email.");
        });

        final var invitation = new InvitationEntity();
        invitation.setId(UUID.randomUUID());
        invitation.setAccount(account);
        invitation.setEmail(email.toLowerCase());
        invitation.setInvitedBy(invitedBy);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(OffsetDateTime.now().plusDays(7));

        final var saved = invitationRepository.save(invitation);

        sendInvitationEmail(saved);

        return saved;
    }

    /**
     * Removes a member from the team.
     *
     * @param account  the account to remove from.
     * @param userId   the user to remove.
     */
    @Transactional
    public void removeMember(final AccountEntity account, final UUID userId) {
        final var userAccount = userAccountRepository.findByUserIdAndAccountId(userId, account.getId())
            .orElseThrow(() -> new IllegalArgumentException("User does not belong to this account."));

        if (userAccount.getRoles().contains(Role.ACCOUNT_ADMIN)) {
             // In a real app we might want to check if they are the ONLY admin
             // For now, let's assume we can remove any admin but they can't remove themselves easily from UI
        }

        userAccountRepository.delete(userAccount);
    }

    /**
     * Cancels a pending invitation.
     *
     * @param account      the account.
     * @param invitationId the invitation id.
     */
    @Transactional
    public void cancelInvitation(final AccountEntity account, final UUID invitationId) {
        final var invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));

        if (!invitation.getAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("Invitation does not belong to this account.");
        }

        invitationRepository.delete(invitation);
    }

    /**
     * Accepts an invitation.
     *
     * @param token the invitation token.
     * @param user  the user who is accepting the invitation.
     */
    @Transactional
    public void acceptInvitation(final String token, final UserEntity user) {
        final var invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation token."));

        if (invitation.getAcceptedAt() != null) {
            throw new IllegalStateException("Invitation has already been accepted.");
        }

        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Invitation has expired.");
        }

        // Add user to the account
        final var userAccountId = invitation.getAccount().getId();
        final var userAccount = userAccountRepository.findByUserIdAndAccountId(user.getId(), userAccountId)
            .orElseGet(() -> new UserAccountEntity(user, invitation.getAccount(), new HashSet<>(List.of(Role.USER))));
        
        userAccount.setLastUsedAt(OffsetDateTime.now());
        userAccountRepository.save(userAccount);

        invitation.setAcceptedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        log.info("User {} accepted invitation to account {}", user.getId(), invitation.getAccount().getId());
    }

    /**
     * Switches the current account for the user.
     *
     * @param userId    the user id.
     * @param accountId the account id to switch to.
     * @return provisioned user information with the new account.
     */
    @Transactional
    public UserProvisioningService.ProvisionedUser switchAccount(final UUID userId, final UUID accountId) {
        final var userAccount = userAccountRepository.findByUserIdAndAccountId(userId, accountId)
            .orElseThrow(() -> new IllegalArgumentException("User does not belong to this account."));

        userAccount.setLastUsedAt(OffsetDateTime.now());
        userAccountRepository.save(userAccount);

        return new UserProvisioningService.ProvisionedUser(userId, accountId, userAccount.getAccount().getName(), userAccount.getRoles());
    }

    private void sendInvitationEmail(final InvitationEntity invitation) {
        final var model = Map.<String, Object>of(
            "inviterName", invitation.getInvitedBy().getEmail(),
            "accountName", invitation.getAccount().getName(),
            "inviteUrl", properties.gateway().url() + "/accept-invite?token=" + invitation.getToken()
        );

        emailService.sendTemplate(invitation.getEmail(),
            "You've been invited to join " + invitation.getAccount().getName() + " on Port Buddy",
            "email/team-invite", model);
    }
}
