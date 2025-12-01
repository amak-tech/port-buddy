/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.common.dto.auth.RegisterRequest;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.service.ApiTokenService;
import tech.amak.portbuddy.server.user.UserProvisioningService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to focus on controller logic
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void register_shouldReturnApiKey() throws Exception {
        final var request = new RegisterRequest("test@example.com", "Test User", "password");
        final var userId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var apiKey = "test-api-key";

        when(userProvisioningService.createLocalUser(any(), any(), any()))
            .thenReturn(new UserProvisioningService.ProvisionedUser(userId, accountId));
        
        when(apiTokenService.createToken(userId, "cli-init"))
            .thenReturn(new ApiTokenService.CreatedToken(UUID.randomUUID().toString(), apiKey));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiKey").value(apiKey));
    }
    
    @Test
    void register_shouldReturnBadRequest_whenMissingFields() throws Exception {
        final var request = new RegisterRequest(null, "Test User", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
