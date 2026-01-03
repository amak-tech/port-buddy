/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import java.util.Map;

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

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;

@Slf4j
@Service
public class StripeService {

    private final AppProperties properties;

    public StripeService(final AppProperties properties) {
        this.properties = properties;
        Stripe.apiKey = properties.stripe().apiKey();
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

        if (account.getStripeSubscriptionId() != null) {
            log.info("Account {} already has an active subscription: {}. It will be replaced.",
                account.getId(), account.getStripeSubscriptionId());
        }

        final var customerId = getOrCreateCustomer(account);
        final var stripeProperties = properties.stripe();

        final var priceId = switch (plan) {
            case PRO -> stripeProperties.priceIds().pro();
            case TEAM -> stripeProperties.priceIds().team();
        };

        final var paramsBuilder = com.stripe.param.checkout.SessionCreateParams.builder()
            .setCustomer(customerId)
            .setMode(Mode.SUBSCRIPTION)
            .setSuccessUrl(properties.gateway().url() + "/app/billing?success=true")
            .setCancelUrl(properties.gateway().url() + "/app/billing?canceled=true")
            .addLineItem(LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .build())
            .putMetadata("accountId", account.getId().toString())
            .putMetadata("plan", plan.name());

        if (account.getStripeSubscriptionId() != null) {
            paramsBuilder.putMetadata("oldSubscriptionId", account.getStripeSubscriptionId());
        }

        // If the account already has extra tunnels recorded, include them in the checkout session
        if (account.getExtraTunnels() > 0) {
            paramsBuilder.addLineItem(LineItem.builder()
                .setPrice(stripeProperties.priceIds().extraTunnel())
                .setQuantity((long) account.getExtraTunnels())
                .build());
        }

        final var session = Session.create(paramsBuilder.build());
        log.info("Created checkout session: id={}, url={}", session.getId(), session.getUrl());
        return session.getUrl();
    }

    /**
     * Cancels the given subscription in Stripe.
     *
     * @param subscriptionId the subscription ID to cancel
     * @throws StripeException if Stripe API call fails
     */
    public void cancelSubscription(final String subscriptionId) throws StripeException {
        if (subscriptionId == null) {
            return;
        }
        log.info("Cancelling Stripe subscription: {}", subscriptionId);
        final var subscription = Subscription.retrieve(subscriptionId);
        subscription.cancel();
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
            .setReturnUrl(properties.gateway().url() + "/app/billing")
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
        final var extraTunnelPriceId = properties.stripe().priceIds().extraTunnel();
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
