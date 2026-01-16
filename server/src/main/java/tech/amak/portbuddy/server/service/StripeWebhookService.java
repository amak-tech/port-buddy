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
