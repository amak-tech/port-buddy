/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.ApiTokenService;

@WebMvcTest(TokensController.class)
@AutoConfigureMockMvc
class TokensControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void revoke_shouldReturnNoContent() throws Exception {
        final var userId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var tokenId = "token-123";

        final var user = new UserEntity();
        final var account = new AccountEntity();
        account.setId(accountId);
        user.setAccount(account);
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/tokens/{id}", tokenId)
                .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isNoContent());
    }
}
