/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.UserAccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.security.Oauth2SuccessHandler;
import tech.amak.portbuddy.server.service.StripeService;
import tech.amak.portbuddy.server.service.TeamService;
import tech.amak.portbuddy.server.service.TunnelService;

@RestController
@RequestMapping(path = "/api/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UsersController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TunnelRepository tunnelRepository;
    private final StripeService stripeService;
    private final TunnelService tunnelService;
    private final UserAccountRepository userAccountRepository;
    private final TeamService teamService;
    private final JwtService jwtService;
    private final AppProperties properties;

    /**
     * User details endpoint.
     *
     * @return user details
     */
    @GetMapping("/details")
    @Transactional
    public UserDetailsResponse details(@AuthenticationPrincipal final Jwt jwt) {
        final var user = resolveUser(jwt);
        final var userAccount = resolveUserAccount(jwt);
        final var account = userAccount.getAccount();

        final var details = new UserDetailsResponse();
        final var userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setRoles(userAccount.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        details.setUser(userDto);

        details.setAccount(toAccountDto(account));

        return details;
    }

    /**
     * Updates user profile.
     *
     * @param jwt     principal.
     * @param request request body.
     * @return updated user profile.
     */
    @PatchMapping(path = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public UserDto updateProfile(@AuthenticationPrincipal final Jwt jwt,
                                 @RequestBody final UpdateProfileRequest request) {

        final var user = resolveUser(jwt);
        final var userAccount = resolveUserAccount(jwt);

        final var firstName = normalizeNullable(request == null ? null : request.getFirstName());
        final var lastName = normalizeNullable(request == null ? null : request.getLastName());

        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);

        final var userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setRoles(userAccount.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
        return userDto;
    }

    /**
     * Updates account name.
     *
     * @param jwt     principal.
     * @param request request body.
     * @return updated account name.
     */
    @PatchMapping(path = "/account", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ACCOUNT_ADMIN', 'ADMIN')")
    @Transactional
    public AccountDto updateAccount(@AuthenticationPrincipal final Jwt jwt,
                                    @RequestBody final UpdateAccountRequest request) {
        final var userAccount = resolveUserAccount(jwt);
        final var account = userAccount.getAccount();

        final var name = normalizeNullable(request == null ? null : request.getName());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Account name must not be empty");
        }
        account.setName(name);
        accountRepository.save(account);

        return toAccountDto(account);
    }

    /**
     * Updates the number of extra tunnels for the account.
     *
     * @param jwt     principal.
     * @param request request body.
     * @return updated account details.
     */
    @PatchMapping(path = "/account/tunnels", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ACCOUNT_ADMIN', 'ADMIN')")
    @Transactional
    public AccountDto updateExtraTunnels(@AuthenticationPrincipal final Jwt jwt,
                                         @RequestBody final UpdateTunnelsRequest request) throws StripeException {
        final var userAccount = resolveUserAccount(jwt);
        final var account = userAccount.getAccount();

        final int currentExtra = account.getExtraTunnels();
        final int requestedExtra = request.getExtraTunnels();

        if (requestedExtra < 0) {
            throw new IllegalArgumentException("Extra tunnels count cannot be negative");
        }

        final int diff = requestedExtra - currentExtra;
        if (diff == 0) {
            return toAccountDto(account);
        }

        final int increment = properties.subscriptions().tunnels().increment().get(account.getPlan());

        if (Math.abs(diff) % increment != 0) {
            throw new IllegalArgumentException(
                "For %s plan, tunnels must be changed by multiples of %d"
                    .formatted(account.getPlan(), increment));
        }

        if (requestedExtra > 0 && account.getStripeSubscriptionId() == null) {
            // If they don't have a subscription yet, we allow the PATCH to update the database
            // but the frontend MUST then follow up with a checkout session to actually create the subscription
            // in Stripe.
            account.setExtraTunnels(requestedExtra);
            accountRepository.save(account);
            return toAccountDto(account);
        }

        stripeService.updateExtraTunnels(account, requestedExtra);

        account.setExtraTunnels(requestedExtra);
        accountRepository.save(account);
        tunnelService.enforceTunnelLimit(account);

        return toAccountDto(account);
    }

    /**
     * Returns the list of accounts the user belongs to.
     *
     * @param jwt the JWT token.
     * @return the list of accounts.
     */
    @Transactional
    @GetMapping("/accounts")
    public List<UserAccountDto> getAccounts(@AuthenticationPrincipal final Jwt jwt) {
        final var userId = resolveUserId(jwt);
        return userAccountRepository.findAllByUserId(userId).stream()
            .map(ua -> UserAccountDto.builder()
                .accountId(ua.getAccount().getId())
                .accountName(ua.getAccount().getName())
                .plan(ua.getAccount().getPlan())
                .roles(ua.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .lastUsedAt(ua.getLastUsedAt())
                .build())
            .toList();
    }

    /**
     * Switches the current account.
     *
     * @param jwt       the JWT token.
     * @param accountId the account id to switch to.
     * @return a new JWT token.
     */
    @Transactional
    @PostMapping("/accounts/{id}/switch")
    public Map<String, String> switchAccount(@AuthenticationPrincipal final Jwt jwt,
                                             @PathVariable("id") final UUID accountId) {
        final var userId = resolveUserId(jwt);
        final var provisioned = teamService.switchAccount(userId, accountId);

        final var claims = new java.util.HashMap<String, Object>();
        final var email = jwt.getClaimAsString(Oauth2SuccessHandler.EMAIL_CLAIM);
        if (email != null) {
            claims.put(Oauth2SuccessHandler.EMAIL_CLAIM, email);
        }
        final var name = jwt.getClaimAsString(Oauth2SuccessHandler.NAME_CLAIM);
        if (name != null) {
            claims.put(Oauth2SuccessHandler.NAME_CLAIM, name);
        }
        final var picture = jwt.getClaimAsString(Oauth2SuccessHandler.PICTURE_CLAIM);
        if (picture != null) {
            claims.put(Oauth2SuccessHandler.PICTURE_CLAIM, picture);
        }
        claims.put(Oauth2SuccessHandler.ACCOUNT_ID_CLAIM, provisioned.accountId().toString());
        claims.put(Oauth2SuccessHandler.ACCOUNT_NAME_CLAIM, provisioned.accountName());
        claims.put(Oauth2SuccessHandler.USER_ID_CLAIM, provisioned.userId().toString());

        final var token = jwtService.createToken(claims, provisioned.userId().toString(), provisioned.roles());
        return Map.of("token", token);
    }

    private AccountDto toAccountDto(final tech.amak.portbuddy.server.db.entity.AccountEntity account) {
        final var dto = new AccountDto();
        dto.setId(account.getId().toString());
        dto.setName(account.getName());
        dto.setPlan(account.getPlan());
        dto.setExtraTunnels(account.getExtraTunnels());
        dto.setSubscriptionStatus(account.getSubscriptionStatus());
        dto.setBaseTunnels(properties.subscriptions().tunnels().base().get(account.getPlan()));
        dto.setActiveTunnels((int) tunnelRepository.countByAccountIdAndStatusIn(
            account.getId(), TunnelService.ACTIVE_STATUSES));
        dto.setStripeCustomerId(account.getStripeCustomerId());
        return dto;
    }

    private UserEntity resolveUser(final Jwt jwt) {
        final var userId = resolveUserId(jwt);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found. Id: " + userId));
    }

    private UserAccountEntity resolveUserAccount(final Jwt jwt) {
        final var userId = resolveUserId(jwt);
        final var accountId = JwtService.resolveAccountId(jwt);
        return userAccountRepository.findByUserIdAndAccountId(userId, accountId)
            .orElseThrow(() -> new IllegalArgumentException("User does not belong to this account."));
    }

    private static String normalizeNullable(final String value) {
        if (value == null) {
            return null;
        }
        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Data
    public static class UserDetailsResponse {
        private UserDto user;
        private AccountDto account;
    }

    @Data
    public static class UserDto {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private Set<String> roles;
    }

    @Data
    public static class AccountDto {
        private String id;
        private String name;
        private Plan plan;
        private int extraTunnels;
        private int baseTunnels;
        private int activeTunnels;
        private String subscriptionStatus;
        private String stripeCustomerId;
    }

    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
    }

    @Data
    public static class UpdateAccountRequest {
        private String name;
    }

    @Data
    public static class UpdateTunnelsRequest {
        private int extraTunnels;
    }

    @Data
    @lombok.Builder
    public static class UserAccountDto {
        private UUID accountId;
        private String accountName;
        private Plan plan;
        private Set<String> roles;
        private OffsetDateTime lastUsedAt;
    }
}
