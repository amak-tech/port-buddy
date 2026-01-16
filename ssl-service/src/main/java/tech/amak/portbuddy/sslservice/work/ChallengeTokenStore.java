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
