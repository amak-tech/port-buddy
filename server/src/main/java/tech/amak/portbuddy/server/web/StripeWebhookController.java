/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.StripeEventEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.StripeEventRepository;
import tech.amak.portbuddy.server.mail.EmailService;
import tech.amak.portbuddy.server.service.StripeWebhookService;
import tech.amak.portbuddy.server.service.TunnelService;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final AccountRepository accountRepository;
    private final StripeEventRepository stripeEventRepository;
    private final EmailService emailService;
    private final TunnelService tunnelService;
    private final StripeWebhookService stripeWebhookService;
    private final AppProperties properties;

    /**
     * Handles Stripe webhooks.
     *
     * @param payload   webhook payload
     * @param sigHeader Stripe-Signature header
     * @return response entity
     */
    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
        @RequestBody final String payload,
        @RequestHeader("Stripe-Signature") final String sigHeader) {

        if (sigHeader == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature");
        }

        final Event event;

        try {
            event = stripeWebhookService.constructEvent(payload, sigHeader, properties.stripe().webhookSecret());
        } catch (final SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Received Stripe event: id={}, type={}", event.getId(), event.getType());

        if (stripeEventRepository.existsById(event.getId())) {
            log.info("Stripe event {} already processed, skipping", event.getId());
            return ResponseEntity.ok("");
        }

        final var eventEntity = new StripeEventEntity();
        eventEntity.setId(event.getId());
        eventEntity.setType(event.getType());
        eventEntity.setPayload(payload);
        eventEntity.setStatus("PROCESSING");
        stripeEventRepository.save(eventEntity);

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.updated":
                case "customer.subscription.deleted":
                    handleSubscriptionEvent(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    log.debug("Unhandled event type: {}", event.getType());
            }

            eventEntity.setStatus("PROCESSED");
            eventEntity.setProcessedAt(OffsetDateTime.now());
            stripeEventRepository.save(eventEntity);
            log.info("Successfully processed Stripe event: id={}, type={}",
                event.getId(), event.getType());
        } catch (final Exception e) {
            log.error("Error processing Stripe event: id={}, type={}",
                event.getId(), event.getType(), e);
            eventEntity.setStatus("FAILED");
            eventEntity.setErrorMessage(e.getMessage());
            stripeEventRepository.save(eventEntity);
            // Return 200 to Stripe to avoid retries if we've successfully stored the failure
            // or 500 if we want Stripe to retry. Usually for processed failures 200 is better to avoid infinite loops.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }

        return ResponseEntity.ok("");
    }

    /**
     * Handles checkout session completed events.
     *
     * @param event the event
     */
    private void handleCheckoutSessionCompleted(final Event event) {
        final var session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
        log.info("Processing checkout.session.completed: sessionId={}, customerId={}",
            session.getId(), session.getCustomer());

        final var accountIdStr = session.getMetadata().get("accountId");
        if (accountIdStr == null) {
            log.error("No accountId in session metadata for session {}", session.getId());
            throw new RuntimeException("No accountId in session metadata");
        }

        final var accountId = UUID.fromString(accountIdStr);
        final var planStr = session.getMetadata().get("plan");

        final var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        account.setStripeCustomerId(session.getCustomer());
        account.setStripeSubscriptionId(session.getSubscription());
        account.setSubscriptionStatus("active");
        if (planStr != null) {
            account.setPlan(Plan.valueOf(planStr));
        }
        accountRepository.save(account);
        tunnelService.enforceTunnelLimit(account);
        log.info("Updated account {} with Stripe customer {} and subscription {}",
            accountId, session.getCustomer(), session.getSubscription());

        sendSubscriptionSuccessEmail(account);
    }

    /**
     * Sends a subscription success email.
     *
     * @param account the account
     */
    private void sendSubscriptionSuccessEmail(final AccountEntity account) {
        final var user = account.getUsers().stream().findFirst().orElse(null);
        if (user != null) {
            final var plan = account.getPlan();
            final var baseLimit = properties.subscriptions().tunnels().base().get(plan);

            emailService.sendTemplate(user.getEmail(), "Welcome to " + plan + " - Port Buddy",
                "email/subscription-success", Map.of("name",
                    user.getFirstName() != null ? user.getFirstName() : "there", "plan",
                    plan.name(), "tunnelLimit", baseLimit, "extraTunnels",
                    account.getExtraTunnels(), "portalUrl",
                    properties.gateway().url() + "/app"));
        }
    }

    /**
     * Handles subscription events.
     *
     * @param event the event
     */
    private void handleSubscriptionEvent(final Event event) {
        final var subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        final var customerId = subscription.getCustomer();
        log.info("Processing {}: subscriptionId={}, customerId={}, status={}",
            event.getType(), subscription.getId(), customerId, subscription.getStatus());

        accountRepository.findByStripeCustomerId(customerId).ifPresentOrElse(account -> {
            final var oldPlan = account.getPlan();
            final var oldStatus = account.getSubscriptionStatus();

            account.setSubscriptionStatus(subscription.getStatus());
            account.setStripeSubscriptionId(subscription.getId());

            // Try to extract plan from subscription items if possible
            if (subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
                final var priceId = subscription.getItems().getData().get(0).getPrice().getId();
                // We could use priceId mapping, but metadata is safer if it's there
                final var planMeta = subscription.getMetadata().get("plan");
                if (planMeta != null) {
                    account.setPlan(Plan.valueOf(planMeta));
                }
            }

            final var user = account.getUsers().stream().findFirst().orElse(null);
            final var isNowActive = "active".equals(account.getSubscriptionStatus());
            final var wasActive = "active".equals(oldStatus);
            final var isCanceled = "canceled".equals(account.getSubscriptionStatus());
            final var planChanged = account.getPlan() != oldPlan;

            accountRepository.save(account);
            tunnelService.enforceTunnelLimit(account);
            log.info("Updated subscription status for account {} to {}", account.getId(), subscription.getStatus());

            if (user != null) {
                if (planChanged || (isNowActive && !wasActive)) {
                    final var plan = account.getPlan();
                    final var baseLimit = properties.subscriptions().tunnels().base().get(plan);
                    emailService.sendTemplate(user.getEmail(), "Plan Updated - Port Buddy",
                        "email/plan-changed", Map.of("name",
                            user.getFirstName() != null ? user.getFirstName() : "there",
                            "plan", plan.name(), "tunnelLimit", baseLimit,
                            "extraTunnels", account.getExtraTunnels(), "portalUrl",
                            properties.gateway().url() + "/app"));
                } else if (isCanceled && !"canceled".equals(oldStatus)) {
                    emailService.sendTemplate(user.getEmail(), "Subscription Canceled - Port Buddy",
                        "email/subscription-canceled", Map.of("name",
                            user.getFirstName() != null ? user.getFirstName() : "there",
                            "portalUrl", properties.gateway().url() + "/app/billing"));
                }
            }
        }, () -> log.warn("Account not found for Stripe customer {}", customerId));
    }

    /**
     * Handles payment failed events.
     *
     * @param event the event
     */
    private void handleInvoicePaymentFailed(final Event event) {
        final var invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElseThrow();
        final var customerId = invoice.getCustomer();
        log.warn("Processing invoice.payment_failed: invoiceId={}, customerId={}",
            invoice.getId(), customerId);

        accountRepository.findByStripeCustomerId(customerId).ifPresentOrElse(account -> {
            account.setSubscriptionStatus("past_due");
            accountRepository.save(account);

            final var user = account.getUsers().stream().findFirst().orElse(null);
            if (user != null) {
                log.info("Sending payment failed email to user: {}", user.getEmail());
                emailService.sendTemplate(user.getEmail(), "Payment Failed - Port Buddy",
                    "email/payment-failed", Map.of("name",
                        user.getFirstName() != null ? user.getFirstName() : "there",
                        "amount", String.format("%.2f %s", invoice.getAmountDue() / 100.0,
                            invoice.getCurrency().toUpperCase()),
                        "portalUrl", properties.gateway().url() + "/app/billing"));
            }
        }, () -> log.warn("Account not found for Stripe customer {}", customerId));
    }
}
