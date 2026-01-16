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

package tech.amak.portbuddy.sslservice.service;

import java.net.URL;
import java.security.KeyPair;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.config.AppProperties;

/**
 * Provides ACME Session and Account using configured ACME directory and contact email.
 */
@Service
@RequiredArgsConstructor
public class AcmeClientService {

    private final AppProperties properties;

    /**
     * Creates an ACME {@link Session} for the configured server URL.
     *
     * @return a new Session
     */
    public Session newSession() {
        return new Session(properties.acme().serverUrl());
    }

    /**
     * Logs into an existing ACME account using the provided key, or creates it on-the-fly if not found.
     *
     * @param session the ACME session
     * @param keyPair the ACME account key pair
     * @return logged-in or newly created {@link Account}
     * @throws AcmeException on ACME failures
     */
    public Account loginOrRegister(final Session session, final KeyPair keyPair) throws AcmeException {
        // Try to log in to an existing account identified by the key pair
        try {
            return new AccountBuilder()
                .onlyExisting()
                .useKeyPair(keyPair)
                .create(session);
        } catch (final AcmeException ex) {
            // Not found -> create a new account below
        }

        final var contactEmail = properties.acme().contactEmail();
        final var builder = new AccountBuilder()
            .agreeToTermsOfService()
            .useKeyPair(keyPair);
        if (contactEmail != null && !contactEmail.isEmpty()) {
            builder.addContact("mailto:" + contactEmail);
        }
        return builder.create(session);
    }

    /**
     * Binds an existing ACME order by its location URL using configured account location and the provided key.
     *
     * @param session       ACME session
     * @param keyPair       ACME account key pair
     * @param orderLocation order URL as string
     * @return bound {@link Order}
     * @throws AcmeException on ACME failures
     */
    public Order bindOrder(final Session session,
                           final Account account,
                           final KeyPair keyPair,
                           final String orderLocation) throws AcmeException {
        try {
            final Login login = session.login(account.getLocation(), keyPair);
            return login.bindOrder(new URL(orderLocation));
        } catch (final Exception e) {
            throw new AcmeException("Failed to bind order: " + e.getMessage(), e);
        }
    }
}
