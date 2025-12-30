/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import org.springframework.stereotype.Service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

/**
 * Service for Stripe webhooks.
 */
@Service
public class StripeWebhookService {
    /**
     * Constructs a Stripe event from the payload and signature.
     *
     * @param payload   the payload
     * @param sigHeader the signature header
     * @param secret    the webhook secret
     * @return the event
     * @throws SignatureVerificationException if signature is invalid
     */
    public Event constructEvent(final String payload, final String sigHeader, final String secret)
        throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, secret);
    }
}
