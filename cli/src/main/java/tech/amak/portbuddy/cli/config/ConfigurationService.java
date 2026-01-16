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

package tech.amak.portbuddy.cli.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.ClientConfig;

@Slf4j
public class ConfigurationService {

    private static final String PORT_BUDDY_ENV = "PORT_BUDDY_ENV";
    private static final String PORT_BUDDY_ENV_DEV = "dev";
    private static final String APP_DIR = ".port-buddy";
    private static final String TOKEN_FILE = "token";

    private final String home = System.getProperty("user.home");
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final AtomicReference<ClientConfig> config = new AtomicReference<>();

    public static final ConfigurationService INSTANCE = new ConfigurationService();

    private ConfigurationService() {
        try {
            configureLogging();
            loadConfig();
            loadToken();
        } catch (final Exception e) {
            log.error("Failed to load config: {}", e.toString());
            config.set(new ClientConfig());
        }
    }

    private void configureLogging() {
        final var env = System.getenv(PORT_BUDDY_ENV);

        final var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        final var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.ERROR);

        if (PORT_BUDDY_ENV_DEV.equalsIgnoreCase(env)) {
            final var appLogger = loggerContext.getLogger("tech.amak.portbuddy");
            appLogger.setLevel(Level.DEBUG);
        }
    }

    private void loadConfig() throws IOException {
        final var envPart = Optional.ofNullable(System.getenv(PORT_BUDDY_ENV))
            .map(String::toLowerCase)
            .map("-%s"::formatted)
            .orElse("");

        final var resourceName = "/application%s.yml".formatted(envPart);
        log.debug("Loading config from resource: {}", resourceName);

        try (final var configStream = ConfigurationService.class.getResourceAsStream(resourceName)) {
            if (configStream != null) {
                final var clientConfig = yamlMapper.readValue(configStream, ClientConfig.class);
                log.debug("Loaded config: {}", clientConfig);
                config.set(clientConfig);
            } else {
                log.warn("Resource not found: {}", resourceName);
                if (config.get() == null) {
                    config.set(new ClientConfig());
                }
            }
        }
    }

    private void loadToken() throws IOException {
        final var tokenFile = Path.of(home, APP_DIR, TOKEN_FILE);
        if (Files.exists(tokenFile)) {
            final var token = Files.readString(tokenFile).trim();
            if (!token.isBlank()) {
                config.get().setApiToken(token);
            }
        }
    }

    public ClientConfig getConfig() {
        return config.get();
    }

    public boolean isDev() {
        final var env = System.getenv(PORT_BUDDY_ENV);
        return PORT_BUDDY_ENV_DEV.equalsIgnoreCase(env);
    }

    /**
     * Saves the provided API token to a file located in the user's home directory
     * under the ".port-buddy" directory. If the directory or file does not exist,
     * they are created. File permissions are restricted to the owner on POSIX systems.
     *
     * @param token the API token to be saved. It is stripped of leading and trailing whitespaces
     *              before being written to the file.
     * @throws IOException if an I/O error occurs while creating the directory, file, or writing the token.
     */
    public void saveApiToken(final String token) throws IOException {
        final var dir = Path.of(home, APP_DIR);

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        final var file = dir.resolve(TOKEN_FILE);
        Files.writeString(file, token.strip());
        // Try to restrict permissions on POSIX systems
        try {
            final var perms = new HashSet<PosixFilePermission>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, perms);
        } catch (final UnsupportedOperationException ignore) {
            // Non-POSIX filesystem (e.g., Windows) - best effort only
        }
    }

}
