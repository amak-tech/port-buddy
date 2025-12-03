/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.mail;

import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.server.config.AppProperties;

/**
 * Sends a welcome email to newly registered users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailService {

    private final EmailService emailService;
    private final AppProperties properties;

    /**
     * Handles user created events and sends a welcome email after the transaction is committed.
     *
     * @param event domain event carrying user details
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(final UserCreatedEvent event) {
        try {
            final var webAppUrl = properties.gateway().url();
            final var fullName = buildFullName(event.firstName(), event.lastName());

            final var model = new HashMap<String, Object>();
            model.put("subject", "Welcome to Port Buddy");
            model.put("greeting", fullName == null ? "Welcome to Port Buddy!" : "Welcome, " + fullName + "!");
            model.put("intro", "Let’s get your local and private services online in seconds.");
            model.put("ctaText", "Open My Account");
            model.put("ctaUrl", webAppUrl);
            model.put("webAppUrl", webAppUrl);
            model.put("featuresTitle", "What you can do with Port Buddy");
            model.put("feature1Title", "Expose HTTP apps");
            model.put("feature1Desc", "Share your local web app with a secure public URL (HTTP & WebSocket).");
            model.put("feature2Title", "Share TCP services");
            model.put("feature2Desc",
                "Give teammates access to databases or any TCP service with a temporary public port.");
            model.put("feature3Title", "Simple CLI");
            model.put("feature3Desc", "One command to expose a port. Auth with API token. Hobby plan is free.");

            emailService.sendTemplate(event.email(), "Welcome to Port Buddy", "email/welcome", model);
        } catch (final Exception e) {
            log.warn("Failed to send welcome email: {}", e.getMessage());
        }
    }

    private static String buildFullName(final String firstName, final String lastName) {
        String result = firstName;
        if (result == null && lastName == null) {
            return null;
        }
        if (result == null) {
            result = "";
        }
        if (lastName != null && !lastName.isBlank()) {
            result = (result + " " + lastName).trim();
        }
        return result.isBlank() ? null : result;
    }
}
