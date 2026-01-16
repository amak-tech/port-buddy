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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserAccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;
import tech.amak.portbuddy.server.db.repo.UserAccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.ApiTokenAuthFilter;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.security.Oauth2SuccessHandler;
import tech.amak.portbuddy.server.service.StripeService;
import tech.amak.portbuddy.server.service.TeamService;
import tech.amak.portbuddy.server.service.TunnelService;

@WebMvcTest(UsersController.class)
@ActiveProfiles("test")
public class UsersControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private UsersController usersController;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private TunnelService tunnelService;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private TunnelRepository tunnelRepository;

    @MockitoBean
    private ApiTokenAuthFilter apiTokenAuthFilter;

    @MockitoBean
    private Oauth2SuccessHandler oauth2SuccessHandler;

    @MockitoBean
    private AppProperties properties;

    private UUID accountId;
    private UUID userId;
    private AccountEntity account;
    private UserAccountEntity userAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usersController)
            .setControllerAdvice(new tech.amak.portbuddy.server.web.advice.GlobalExceptionHandler())
            .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(final MethodParameter parameter) {
                    return parameter.getParameterType().equals(Jwt.class);
                }

                @Override
                public Object resolveArgument(final MethodParameter parameter,
                                              final ModelAndViewContainer mavContainer,
                                              final NativeWebRequest webRequest,
                                              final WebDataBinderFactory binderFactory) {
                    final var principal = webRequest.getUserPrincipal();
                    if (principal instanceof JwtAuthenticationToken jwtToken) {
                        return jwtToken.getToken();
                    }
                    return null;
                }
            })
            .build();

        accountId = UUID.randomUUID();
        userId = UUID.randomUUID();

        account = new AccountEntity();
        account.setId(accountId);
        account.setName("Test Account");
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(0);

        userAccount = new UserAccountEntity();
        userAccount.setAccount(account);

        when(userAccountRepository.findByUserIdAndAccountId(eq(userId), eq(accountId)))
            .thenReturn(Optional.of(userAccount));

        final var subscriptions = new AppProperties.Subscriptions(
            null,
            null,
            new AppProperties.Subscriptions.Tunnels(
                Map.of(Plan.PRO, 1, Plan.TEAM, 10),
                Map.of(Plan.PRO, 1, Plan.TEAM, 5)
            )
        );
        when(properties.subscriptions()).thenReturn(subscriptions);
    }

    @Test
    void updateExtraTunnels_withoutSubscription_shouldReturnCheckoutUrl() throws Exception {
        final var request = new UsersController.UpdateTunnelsRequest();
        request.setExtraTunnels(1);

        final var checkoutUrl = "https://checkout.stripe.com/test";
        when(stripeService.createCheckoutSession(any(), eq(Plan.PRO), eq(1))).thenReturn(checkoutUrl);

        mockMvc.perform(patch("/api/users/me/account/tunnels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new JwtAuthenticationToken(createJwt(List.of("ACCOUNT_ADMIN")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.extraTunnels").value(0)) // Should still be 0 in the response as it's not saved
            .andExpect(jsonPath("$.checkoutUrl").value(checkoutUrl));

        verify(accountRepository, org.mockito.Mockito.never()).save(account);
    }

    @Test
    void updateExtraTunnels_withSubscription_shouldUpdateStripe() throws Exception {
        account.setStripeSubscriptionId("sub_123");

        final var request = new UsersController.UpdateTunnelsRequest();
        request.setExtraTunnels(1);

        mockMvc.perform(patch("/api/users/me/account/tunnels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new JwtAuthenticationToken(createJwt(List.of("ACCOUNT_ADMIN")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.extraTunnels").value(1))
            .andExpect(jsonPath("$.checkoutUrl").isEmpty());

        verify(stripeService).updateExtraTunnels(account, 1);
        verify(accountRepository).save(account);
    }

    @Test
    void updateExtraTunnels_proPlan_zeroExtra_withSubscription_shouldCancelSubscription() throws Exception {
        account.setStripeSubscriptionId("sub_123");
        account.setPlan(Plan.PRO);
        account.setExtraTunnels(1);

        final var request = new UsersController.UpdateTunnelsRequest();
        request.setExtraTunnels(0);

        mockMvc.perform(patch("/api/users/me/account/tunnels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new JwtAuthenticationToken(createJwt(List.of("ACCOUNT_ADMIN")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.extraTunnels").value(0))
            .andExpect(jsonPath("$.subscriptionStatus").value("canceled"));

        verify(stripeService).cancelSubscription(account);
        verify(accountRepository).save(account);
    }

    private Jwt createJwt(final List<String> roles) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("aid", accountId.toString())
            .claim("roles", roles)
            .build();
    }
}
