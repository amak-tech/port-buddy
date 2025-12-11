/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.work;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory storage for ACME HTTP-01 challenge tokens.
 */
@Component
public class ChallengeTokenStore {

    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    /**
     * Adds or updates a challenge token value.
     *
     * @param token token name
     * @param content token content
     */
    public void putToken(final String token, final String content) {
        tokens.put(token, content);
    }

    /**
     * Retrieves challenge token content by token name.
     *
     * @param token token name
     * @return token content or null
     */
    public String getTokenContent(final String token) {
        return tokens.get(token);
    }

    /**
     * Removes a token from the store.
     *
     * @param token token name
     */
    public void removeToken(final String token) {
        tokens.remove(token);
    }
}
