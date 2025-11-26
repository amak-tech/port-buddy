/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.util.UUID;

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
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;

@RestController
@RequestMapping(path = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * User details endpoint.
     *
     * @return user details
     */
    @GetMapping("/details")
    public DetailsResponse details(@AuthenticationPrincipal final Jwt jwt) {
        final var ids = resolveIds(jwt);
        final var user = userRepository.findById(ids.userId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        final var account = accountRepository.findById(ids.accountId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

        final var details = new DetailsResponse();
        final var userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setAvatarUrl(user.getAvatarUrl());
        details.setUser(userDto);

        final var accountDto = new AccountDto();
        accountDto.setId(account.getId().toString());
        accountDto.setName(account.getName());
        accountDto.setPlan(account.getPlan());
        details.setAccount(accountDto);

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
        final var ids = resolveIds(jwt);
        final var user = userRepository.findById(ids.userId())
            .orElseThrow(() -> new RuntimeException("User not found"));

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
        final var ids = resolveIds(jwt);
        final var account = accountRepository.findById(ids.accountId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

        final var name = normalizeNullable(request == null ? null : request.getName());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Account name must not be empty");
        }
        account.setName(name);
        accountRepository.save(account);

        final var dto = new AccountDto();
        dto.setId(account.getId().toString());
        dto.setName(account.getName());
        dto.setPlan(account.getPlan());
        return dto;
    }

    private static Ids resolveIds(final Jwt jwt) {
        final var userId = UUID.fromString(jwt.getSubject());
        final var accountIdClaim = jwt.getClaimAsString("aid");
        final var accountId = accountIdClaim != null ? UUID.fromString(accountIdClaim) : null;
        if (accountId == null) {
            throw new RuntimeException("Missing account id in token");
        }
        return new Ids(userId, accountId);
    }

    private static String normalizeNullable(final String value) {
        if (value == null) {
            return null;
        }
        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Ids(UUID userId, UUID accountId) {
    }

    @Data
    public static class DetailsResponse {
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
    }

    @Data
    public static class AccountDto {
        private String id;
        private String name;
        private String plan;
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
}
