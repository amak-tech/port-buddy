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

package tech.amak.portbuddy.server.web;

import static tech.amak.portbuddy.server.security.JwtService.resolveAccountId;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.exception.StripeException;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.Plan;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.service.StripeService;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_ADMIN')")
public class PaymentController {

    private final StripeService stripeService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * Creates a checkout session for the user's account and the requested plan.
     *
     * @param jwt     the JWT token
     * @param request the checkout request containing the plan
     * @return a response containing the checkout session URL
     * @throws StripeException if Stripe API call fails
     */
    @Transactional
    @PostMapping("/create-checkout-session")
    public SessionResponse createCheckoutSession(
        @AuthenticationPrincipal final Jwt jwt,
        @RequestBody final CheckoutRequest request) throws StripeException {
        final var accountId = resolveAccountId(jwt);
        final var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        final var url = stripeService.createCheckoutSession(account, request.getPlan());
        return new SessionResponse(url);
    }

    /**
     * Creates a billing portal session for the user's account.
     *
     * @param jwt the JWT token
     * @return a response containing the billing portal session URL
     * @throws StripeException if Stripe API call fails
     */
    @Transactional
    @PostMapping("/create-portal-session")
    public SessionResponse createPortalSession(@AuthenticationPrincipal final Jwt jwt) throws StripeException {
        final var accountId = resolveAccountId(jwt);
        final var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        final var url = stripeService.createPortalSession(account);
        return new SessionResponse(url);
    }

    /**
     * Cancels the current subscription for the user's account and resets extra tunnels.
     *
     * @param jwt the JWT token
     * @throws StripeException if Stripe API call fails
     */
    @Transactional
    @PostMapping("/cancel-subscription")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSubscription(@AuthenticationPrincipal final Jwt jwt) throws StripeException {
        final var accountId = resolveAccountId(jwt);
        final var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        stripeService.cancelSubscription(account);
        account.setExtraTunnels(0);
        account.setPlan(Plan.PRO);
        account.setSubscriptionStatus("active");
        account.setStripeSubscriptionId(null);
        accountRepository.save(account);
    }

    @Data
    public static class CheckoutRequest {
        private Plan plan;
    }

    @Data
    @RequiredArgsConstructor
    public static class SessionResponse {
        private final String url;
    }
}
