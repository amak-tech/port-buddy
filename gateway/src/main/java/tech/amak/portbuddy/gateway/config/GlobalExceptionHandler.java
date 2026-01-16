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

package tech.amak.portbuddy.gateway.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AppProperties properties;

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleIllegalArgumentException(final ResponseStatusException ex) {
        log.error("Status exception: Status: {} - {}", ex.getStatusCode(), ex.getMessage());
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getMessage());
    }

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
