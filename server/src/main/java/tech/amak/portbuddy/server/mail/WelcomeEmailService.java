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
            if (event.passwordResetLink() != null) {
                model.put("ctaText", "Setup My Account");
                model.put("ctaUrl", event.passwordResetLink());
            } else {
                model.put("ctaText", "Open My Account");
                model.put("ctaUrl", webAppUrl + "/app");
            }
            model.put("webAppUrl", webAppUrl);
            model.put("featuresTitle", "What you can do with Port Buddy");
            model.put("feature1Title", "Expose HTTP apps");
            model.put("feature1Desc", "Share your local web app with a secure public URL (HTTP & WebSocket).");
            model.put("feature2Title", "Share TCP services");
            model.put("feature2Desc",
                "Give teammates access to databases or any TCP service with a temporary public port.");
            model.put("feature3Title", "Simple CLI");
            model.put("feature3Desc", "One command to expose a port. Auth with API token. Pro plan is free.");

            log.info("sending welcome emails to user {}", event.userId());
            emailService.sendTemplate(event.email(), "Welcome to Port Buddy", "email/welcome", model);
            log.info("welcome emails to user {} is sent", event.userId());
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
