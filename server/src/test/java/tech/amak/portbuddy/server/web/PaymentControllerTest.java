/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.ApiTokenAuthFilter;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.security.Oauth2SuccessHandler;
import tech.amak.portbuddy.server.service.StripeService;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
public class PaymentControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private PaymentController paymentController;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ApiTokenAuthFilter apiTokenAuthFilter;

    @MockitoBean
    private Oauth2SuccessHandler oauth2SuccessHandler;

    private UUID accountId;
    private UUID userId;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
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

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    }

    @Test
    void createCheckoutSession_WithAccountAdmin_ShouldSucceed() throws Exception {
        when(stripeService.createCheckoutSession(any(), any())).thenReturn("https://checkout.stripe.com/test");

        final var request = new PaymentController.CheckoutRequest();
        request.setPlan(Plan.PRO);

        mockMvc.perform(post("/api/payments/create-checkout-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new JwtAuthenticationToken(createJwt(List.of("ACCOUNT_ADMIN")))))
            .andExpect(status().isOk());
    }

    @Test
    void createPortalSession_WithAdmin_ShouldSucceed() throws Exception {
        when(stripeService.createPortalSession(any())).thenReturn("https://billing.stripe.com/test");

        mockMvc.perform(post("/api/payments/create-portal-session")
                .principal(new JwtAuthenticationToken(createJwt(List.of("ADMIN")))))
            .andExpect(status().isOk());
    }

    @Test
    void cancelSubscription_shouldUpdateAccount() throws Exception {
        account.setExtraTunnels(5);
        account.setSubscriptionStatus("active");
        account.setStripeSubscriptionId("sub_123");

        mockMvc.perform(post("/api/payments/cancel-subscription")
                .principal(new JwtAuthenticationToken(createJwt(List.of("ACCOUNT_ADMIN")))))
            .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(stripeService).cancelSubscription(account);
        org.mockito.Mockito.verify(accountRepository).save(account);
        
        org.junit.jupiter.api.Assertions.assertEquals(0, account.getExtraTunnels());
        org.junit.jupiter.api.Assertions.assertEquals("canceled", account.getSubscriptionStatus());
        org.junit.jupiter.api.Assertions.assertNull(account.getStripeSubscriptionId());
    }

    // Note: standaloneSetup doesn't enforce @PreAuthorize. 
    // To test @PreAuthorize we would need a full integration test or a different setup.
    // However, since I'm just verifying the controller logic works when authorized, 
    // and I've manually added @PreAuthorize, this is a good start.

    private Jwt createJwt(final List<String> roles) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("aid", accountId.toString())
            .claim("roles", roles)
            .build();
    }
}
