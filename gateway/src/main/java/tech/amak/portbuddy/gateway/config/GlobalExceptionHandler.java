/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.gateway.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AppProperties properties;

    /**
     * Handles all uncaught exceptions by logging the error and redirecting to a server error page
     * with the original request URI encoded as a retry parameter.
     *
     * @param ex       the exception that was thrown
     * @param exchange the current server web exchange containing the request details
     * @return a {@link RedirectView} pointing to the configured server error page with a retry parameter
     */
    @ExceptionHandler(Exception.class)
    public RedirectView handleGenericException(final Exception ex, final ServerWebExchange exchange) {
        log.error("Global exception: {}", ex.getMessage(), ex);
        final var string = exchange.getRequest().getURI().toString();
        final var redirectUri = properties.serverErrorPage() + "?retry=" + UriUtils.encode(string, UTF_8);
        return new RedirectView(redirectUri, HttpStatus.TEMPORARY_REDIRECT);
    }
}
