/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.web;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.sslservice.work.ChallengeTokenStore;

@RestController
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeTokenStore challengeTokenStore;

    /**
     * Serves HTTP-01 ACME challenge tokens.
     *
     * @param token token name
     * @return token content or empty string if not found
     */
    @GetMapping(value = "/.well-known/acme-challenge/{token}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getChallengeToken(@PathVariable("token") final String token) {
        final var content = challengeTokenStore.getTokenContent(token);
        final var body = content == null ? "" : content;
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
            .body(body);
    }
}
