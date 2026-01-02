/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.InvitationEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.Oauth2SuccessHandler;
import tech.amak.portbuddy.server.service.TeamService;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Transactional
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Returns a list of team members for the current user's account.
     *
     * @param jwt the JWT token
     * @return the list of team members
     */
    @GetMapping("/members")
    public List<MemberDto> getMembers(@AuthenticationPrincipal final Jwt jwt) {
        final var account = getAccount(jwt);
        return teamService.getMembers(account).stream()
            .map(member -> toMemberDto(member, account))
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of pending invitations for the current user's account.
     *
     * @param jwt the JWT token
     * @return the list of pending invitations
     */
    @GetMapping("/invitations")
    public List<InvitationDto> getInvitations(@AuthenticationPrincipal final Jwt jwt) {
        final var account = getAccount(jwt);
        return teamService.getPendingInvitations(account).stream()
            .map(this::toInvitationDto)
            .collect(Collectors.toList());
    }

    /**
     * Invites a new member to the current user's account.
     *
     * @param jwt     the JWT token
     * @param request the invite request
     * @return the created invitation
     */
    @PostMapping("/invitations")
    @PreAuthorize("hasRole('ACCOUNT_ADMIN')")
    public InvitationDto inviteMember(@AuthenticationPrincipal final Jwt jwt,
                                      @RequestBody final InviteRequest request) {
        final var account = getAccount(jwt);
        final var user = getUser(jwt);
        final var invitation = teamService.inviteMember(account, user, request.getEmail());
        return toInvitationDto(invitation);
    }

    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasRole('ACCOUNT_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(@AuthenticationPrincipal final Jwt jwt,
                                 @PathVariable("id") final UUID id) {
        final var account = getAccount(jwt);
        teamService.cancelInvitation(account, id);
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvitation(@AuthenticationPrincipal final Jwt jwt,
                                 @RequestParam("token") final String token) {
        final var user = getUser(jwt);
        teamService.acceptInvitation(token, user);
    }

    private AccountEntity getAccount(final Jwt jwt) {
        final var accountIdClaim = jwt.getClaimAsString(Oauth2SuccessHandler.ACCOUNT_ID_CLAIM);
        if (accountIdClaim == null) {
            throw new IllegalArgumentException("Account ID claim is missing.");
        }
        final var accountId = UUID.fromString(accountIdClaim);
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    private UserEntity getUser(final Jwt jwt) {
        final var userId = UUID.fromString(jwt.getSubject());
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private MemberDto toMemberDto(final UserEntity user, final AccountEntity account) {
        final var userAccount = userAccountRepository.findByUserIdAndAccountId(user.getId(), account.getId())
            .orElseThrow(() -> new IllegalArgumentException("User does not belong to this account."));
        return MemberDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .avatarUrl(user.getAvatarUrl())
            .roles(userAccount.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
            .joinedAt(user.getCreatedAt())
            .build();
    }

    private InvitationDto toInvitationDto(final InvitationEntity invitation) {
        return InvitationDto.builder()
            .id(invitation.getId())
            .email(invitation.getEmail())
            .invitedBy(invitation.getInvitedBy().getEmail())
            .createdAt(invitation.getCreatedAt())
            .expiresAt(invitation.getExpiresAt())
            .build();
    }

    @Data
    @Builder
    public static class MemberDto {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private java.util.Set<String> roles;
        private OffsetDateTime joinedAt;
    }

    @Data
    @Builder
    public static class InvitationDto {
        private UUID id;
        private String email;
        private String invitedBy;
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;
    }

    @Data
    public static class InviteRequest {
        private String email;
    }
}
