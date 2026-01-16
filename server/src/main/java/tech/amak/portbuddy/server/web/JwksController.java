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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;
import tech.amak.portbuddy.common.dto.jwks.JwkKey;
import tech.amak.portbuddy.common.dto.jwks.JwksResponse;
import tech.amak.portbuddy.server.security.RsaKeyProvider;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyProvider rsaKeyProvider;

    /**
     * Provides the public JSON Web Key Set (JWKS) for the application.
     * This endpoint exposes the keys that clients can use to validate
     * the signatures of issued JSON Web Tokens (JWTs).
     * The result is cacheable for up to 5 minutes for performance optimization
     * and convenience of clients consuming this endpoint.
     *
     * @return a `ResponseEntity` containing a `JwksResponse` object which
     *         holds a list of the public JSON Web Keys available for verification.
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JwksResponse> jwks() {
        final var set = rsaKeyProvider.getPublicJwkSet();
        final List<JwkKey> keys = new ArrayList<>();
        for (final JWK jwk : set.getKeys()) {
            if (jwk instanceof RSAKey rsa) {
                final var pub = rsa.toPublicJWK();
                final var json = pub.toJSONObject();
                final var dto = new JwkKey();
                dto.setKty((String) json.get("kty"));
                dto.setKid((String) json.get("kid"));
                dto.setUse((String) json.get("use"));
                dto.setAlg((String) json.get("alg"));
                dto.setModulus((String) json.get("n"));
                dto.setExponent((String) json.get("e"));
                keys.add(dto);
            }
        }
        final var body = new JwksResponse(keys);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(body);
    }
}
