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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.config.AppProperties;

/**
 * Loads or creates ACME account key pair from configured location.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcmeAccountService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;

    /**
     * Loads the ACME account {@link KeyPair} from {@code app.acme.accountKeyPath}.
     * The key must be in PEM format (PKCS#1 or PKCS#8). If the file does not exist,
     * a new key pair is generated and saved.
     *
     * @return account key pair
     */
    public KeyPair loadAccountKeyPair() {
        final var pathString = properties.acme().accountKeyPath();
        final var resource = resourceLoader.getResource(pathString);

        if (resource.exists()) {
            try (final var reader = new InputStreamReader(resource.getInputStream())) {
                return KeyPairUtils.readKeyPair(reader);
            } catch (final IOException e) {
                log.error("Failed to load ACME account key pair from {}", pathString, e);
                throw new IllegalStateException("Failed to load ACME account key pair", e);
            }
        } else {
            final var keyPair = KeyPairUtils.createKeyPair();
            try {
                final var file = resource.getFile();
                final var parentFile = file.getParentFile();
                if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentFile);
                }
                try (final var writer = Files.newBufferedWriter(file.toPath())) {
                    KeyPairUtils.writeKeyPair(keyPair, writer);
                }
            } catch (final IOException e) {
                log.error("Failed to save ACME account key pair to {}", pathString, e);
                throw new IllegalStateException("Failed to save ACME account key pair", e);
            }
            return keyPair;
        }
    }
}
