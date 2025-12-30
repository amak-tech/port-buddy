/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.db.entity.AccountEntity;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.price-ids.pro}")
    private String proPriceId;

    @Value("${stripe.price-ids.team}")
    private String teamPriceId;

    @Value("${stripe.price-ids.extra-tunnel}")
    private String extraTunnelPriceId;

    @Value("${app.gateway.url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    /**
     * Creates a checkout session for the given account and plan.
     *
     * @param account the account
     * @param plan    the plan
     * @return the checkout session URL
     * @throws StripeException if Stripe API call fails
     */
    public String createCheckoutSession(final AccountEntity account, final Plan plan) throws StripeException {
        log.info("Creating checkout session for account: {}, plan: {}", account.getId(), plan);
        final var customerId = getOrCreateCustomer(account);

        final var priceId = switch (plan) {
            case PRO -> proPriceId;
            case TEAM -> teamPriceId;
        };

        final var paramsBuilder = com.stripe.param.checkout.SessionCreateParams.builder()
            .setCustomer(customerId)
            .setMode(Mode.SUBSCRIPTION)
            .setSuccessUrl(baseUrl + "/app/billing?success=true")
            .setCancelUrl(baseUrl + "/app/billing?canceled=true")
            .addLineItem(LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .build())
            .putMetadata("accountId", account.getId().toString())
            .putMetadata("plan", plan.name());

        // If the account already has extra tunnels recorded, include them in the checkout session
        if (account.getExtraTunnels() > 0) {
            paramsBuilder.addLineItem(LineItem.builder()
                .setPrice(extraTunnelPriceId)
                .setQuantity((long) account.getExtraTunnels())
                .build());
        }

        final var session = Session.create(paramsBuilder.build());
        log.info("Created checkout session: id={}, url={}", session.getId(), session.getUrl());
        return session.getUrl();
    }

    /**
     * Creates a billing portal session for the given account.
     *
     * @param account the account
     * @return the billing portal session URL
     * @throws StripeException if Stripe API call fails
     */
    public String createPortalSession(final AccountEntity account) throws StripeException {
        log.info("Creating billing portal session for account: {}, customer: {}", account.getId(),
            account.getStripeCustomerId());
        final var params = SessionCreateParams.builder()
            .setCustomer(account.getStripeCustomerId())
            .setReturnUrl(baseUrl + "/app/billing")
            .build();

        final var session = com.stripe.model.billingportal.Session.create(params);
        log.info("Created billing portal session: id={}, url={}", session.getId(), session.getUrl());
        return session.getUrl();
    }

    /**
     * Updates the number of extra tunnels for the given account.
     *
     * @param account  the account
     * @param newCount the new count of extra tunnels
     * @throws StripeException if Stripe API call fails
     */
    public void updateExtraTunnels(final AccountEntity account, final int newCount) throws StripeException {
        log.info("Updating extra tunnels for account: {}, newCount: {}", account.getId(), newCount);
        if (account.getStripeSubscriptionId() == null) {
            log.warn("Account {} has no Stripe subscription, cannot update tunnels in Stripe", account.getId());
            return;
        }

        final var subscription = Subscription.retrieve(account.getStripeSubscriptionId());
        final var subscriptionItemId = subscription.getItems().getData().stream()
            .filter(item -> item.getPrice().getId().equals(extraTunnelPriceId))
            .map(com.stripe.model.SubscriptionItem::getId)
            .findFirst()
            .orElse(null);

        final var paramsBuilder = SubscriptionUpdateParams.builder()
            .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE);

        if (subscriptionItemId != null) {
            if (newCount == 0) {
                log.info("Removing extra tunnels item from subscription: {}", account.getStripeSubscriptionId());
                // Remove the extra tunnels item
                paramsBuilder.addItem(SubscriptionUpdateParams.Item.builder()
                    .setId(subscriptionItemId)
                    .setDeleted(true)
                    .build());
            } else {
                log.info("Updating extra tunnels quantity to {} for subscription: {}", newCount,
                    account.getStripeSubscriptionId());
                // Update quantity
                paramsBuilder.addItem(SubscriptionUpdateParams.Item.builder()
                    .setId(subscriptionItemId)
                    .setQuantity((long) newCount)
                    .build());
            }
        } else if (newCount > 0) {
            log.info("Adding extra tunnels item (quantity: {}) to subscription: {}", newCount,
                account.getStripeSubscriptionId());
            // Add extra tunnels item
            paramsBuilder.addItem(SubscriptionUpdateParams.Item.builder()
                .setPrice(extraTunnelPriceId)
                .setQuantity((long) newCount)
                .build());
        } else {
            log.debug("No changes needed for extra tunnels on subscription: {}", account.getStripeSubscriptionId());
            return;
        }

        subscription.update(paramsBuilder.build());
        log.info("Successfully updated Stripe subscription for account: {}", account.getId());
    }

    private String getOrCreateCustomer(final AccountEntity account) throws StripeException {
        if (account.getStripeCustomerId() != null) {
            log.debug("Using existing Stripe customer {} for account {}", account.getStripeCustomerId(),
                account.getId());
            return account.getStripeCustomerId();
        }

        log.info("Creating new Stripe customer for account: {}", account.getId());
        final var params = CustomerCreateParams.builder()
            .setName(account.getName())
            .setMetadata(Map.of("accountId", account.getId().toString()))
            .build();

        final var customer = Customer.create(params);
        log.info("Created Stripe customer: id={}", customer.getId());
        return customer.getId();
    }
}
