/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveUserId;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;

@RestController
@RequestMapping(path = "/api/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UsersController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * User details endpoint.
     *
     * @return user details
     */
    @GetMapping("/details")
    @Transactional
    public UserDetailsResponse details(@AuthenticationPrincipal final Jwt jwt) {

        final var user = resolveUser(jwt);
        final var account = user.getAccount();

        final var details = new UserDetailsResponse();
        final var userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setAvatarUrl(user.getAvatarUrl());
        userDto.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
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
        userDto.setRoles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
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
    @Transactional
    public AccountDto updateAccount(@AuthenticationPrincipal final Jwt jwt,
                                    @RequestBody final UpdateAccountRequest request) {
        final var user = resolveUser(jwt);
        final var account = user.getAccount();

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
    @Transactional
    public AccountDto updateExtraTunnels(@AuthenticationPrincipal final Jwt jwt,
                                         @RequestBody final UpdateTunnelsRequest request) {
        final var user = resolveUser(jwt);
        final var account = user.getAccount();

        final int currentExtra = account.getExtraTunnels();
        final int requestedExtra = request.getExtraTunnels();

        if (requestedExtra < 0) {
            throw new IllegalArgumentException("Extra tunnels count cannot be negative");
        }

        final int diff = requestedExtra - currentExtra;
        if (diff == 0) {
            return toAccountDto(account);
        }

        final int increment = switch (account.getPlan()) {
            case PRO -> 1;
            case TEAM -> 5;
        };

        if (Math.abs(diff) % increment != 0) {
            throw new IllegalArgumentException(
                "For %s plan, tunnels must be changed by multiples of %d"
                    .formatted(account.getPlan(), increment));
        }

        account.setExtraTunnels(requestedExtra);
        accountRepository.save(account);

        return toAccountDto(account);
    }

    private AccountDto toAccountDto(final tech.amak.portbuddy.server.db.entity.AccountEntity account) {
        final var dto = new AccountDto();
        dto.setId(account.getId().toString());
        dto.setName(account.getName());
        dto.setPlan(account.getPlan());
        dto.setExtraTunnels(account.getExtraTunnels());
        dto.setBaseTunnels(switch (account.getPlan()) {
            case PRO -> 1;
            case TEAM -> 10;
        });
        return dto;
    }

    private UserEntity resolveUser(final Jwt jwt) {
        final var userId = resolveUserId(jwt);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found. Id: " + userId));
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
}
