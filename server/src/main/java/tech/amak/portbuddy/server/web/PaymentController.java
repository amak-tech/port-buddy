/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
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
