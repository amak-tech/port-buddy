/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.StripeEventRepository;
import tech.amak.portbuddy.server.mail.EmailService;
import tech.amak.portbuddy.server.service.StripeWebhookService;
import tech.amak.portbuddy.server.service.TunnelService;

import com.stripe.model.Event;
import com.stripe.model.EventDataDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.Price;

import java.util.Map;

@WebMvcTest(StripeWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private StripeEventRepository stripeEventRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private TunnelService tunnelService;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        when(appProperties.gateway()).thenReturn(new AppProperties.Gateway(
                "http://localhost:8080", "localhost", "http", "/404", "/passcode"
        ));
    }

    @Test
    void handleInvoicePaymentFailed_shouldSendEmail() throws Exception {
        final var customerId = "cus_123";
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setStripeCustomerId(customerId);
        account.setPlan(Plan.PRO);

        final var user = new UserEntity();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        account.setUsers(List.of(user));

        when(accountRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(account));
        when(stripeEventRepository.existsById(anyString())).thenReturn(false);

        final var invoice = new Invoice();
        invoice.setCustomer(customerId);
        invoice.setAmountDue(1000L);
        invoice.setCurrency("usd");

        final var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_123");
        when(event.getType()).thenReturn("invoice.payment_failed");
        
        final var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripeWebhookService.constructEvent(anyString(), anyString(), any())).thenReturn(event);

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(emailService).sendTemplate(
                eq("test@example.com"),
                eq("Payment Failed - Port Buddy"),
                eq("email/payment-failed"),
                anyMap()
        );
    }

    @Test
    void handleSubscriptionDeleted_shouldSendEmail() throws Exception {
        final var customerId = "cus_123";
        final var account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setStripeCustomerId(customerId);
        account.setPlan(Plan.PRO);
        account.setSubscriptionStatus("active");

        final var user = new UserEntity();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        account.setUsers(List.of(user));

        when(accountRepository.findByStripeCustomerId(customerId)).thenReturn(Optional.of(account));
        when(stripeEventRepository.existsById(anyString())).thenReturn(false);

        final var subscription = new Subscription();
        subscription.setCustomer(customerId);
        subscription.setStatus("canceled");

        final var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_456");
        when(event.getType()).thenReturn("customer.subscription.deleted");

        final var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripeWebhookService.constructEvent(anyString(), anyString(), any())).thenReturn(event);

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(emailService).sendTemplate(
                eq("test@example.com"),
                eq("Subscription Canceled - Port Buddy"),
                eq("email/subscription-canceled"),
                anyMap()
        );
    }
}
