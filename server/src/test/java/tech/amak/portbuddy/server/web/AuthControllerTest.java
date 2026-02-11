/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
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
import tech.amak.portbuddy.common.dto.auth.TokenExchangeRequest;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.Role;
import tech.amak.portbuddy.server.db.entity.UserAccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.service.ApiTokenService;
import tech.amak.portbuddy.server.service.user.PasswordResetService;
import tech.amak.portbuddy.server.service.user.UserProvisioningService;
import tech.amak.portbuddy.server.service.user.UserProvisioningService.ProvisionedUser;
import tech.amak.portbuddy.server.web.dto.LoginRequest;
import tech.amak.portbuddy.server.web.dto.PasswordResetConfirm;
import tech.amak.portbuddy.server.web.dto.PasswordResetRequest;

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

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private AppProperties appProperties;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void register_shouldReturnApiKey() throws Exception {
        final var request = new RegisterRequest("test@example.com", "Test User", "password");
        final var userId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var apiKey = "test-api-key";

        when(userProvisioningService.createLocalUser(any(), any(), any()))
            .thenReturn(new ProvisionedUser(userId, accountId, "Test Account", Set.of(Role.ACCOUNT_ADMIN)));

        when(apiTokenService.createToken(accountId, userId, "prtb-client"))
            .thenReturn(new ApiTokenService.CreatedToken(UUID.randomUUID().toString(), apiKey));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiKey").value(apiKey))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_shouldReturnError_whenMissingFields() throws Exception {
        final var request = new RegisterRequest(null, "Test User", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk()) // We now return 200 with error details
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Email is required"))
            .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void requestPasswordReset_shouldReturnNoContent() throws Exception {
        final var request = new PasswordResetRequest("test@example.com");

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }

    @Test
    void tokenExchange_shouldReturnJwt() throws Exception {
        final var apiToken = "valid-token";
        final var clientVersion = "1.0.0";
        final var request = new TokenExchangeRequest(apiToken, clientVersion);
        final var accountId = UUID.randomUUID();
        final var userId = UUID.randomUUID();
        final var apiKeyId = UUID.randomUUID();

        when(appProperties.cli()).thenReturn(new AppProperties.Cli("1.0.0"));
        when(apiTokenService.validateAndGetApiKey(apiToken))
            .thenReturn(Optional.of(new ApiTokenService.ValidatedApiKey(userId, accountId, apiKeyId)));

        final var account = new AccountEntity();
        account.setId(accountId);
        account.setBlocked(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        when(jwtService.createToken(any(), any())).thenReturn("test-jwt");

        mockMvc.perform(post("/api/auth/token-exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("test-jwt"));
    }

    @Test
    void login_shouldReturnJwt() throws Exception {
        final var email = "test@example.com";
        final var password = "password";
        final var request = new LoginRequest(email, password);
        final var userId = UUID.randomUUID();

        final var user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setPassword("hashed-password");

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed-password")).thenReturn(true);

        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setBlocked(false);

        final var userAccount = new UserAccountEntity(user, account, Set.of(Role.ACCOUNT_ADMIN));
        when(userAccountRepository.findLatestUsedByUserId(userId)).thenReturn(Optional.of(userAccount));
        when(jwtService.createToken(any(), any(), any())).thenReturn("login-jwt");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("login-jwt"));
    }

    @Test
    void confirmPasswordReset_shouldReturnNoContent() throws Exception {
        final var request = new PasswordResetConfirm("valid-token", "new-password");

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
    }
}
