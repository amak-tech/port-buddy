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

package tech.amak.portbuddy.cli;

import static tech.amak.portbuddy.cli.utils.JsonUtils.MAPPER;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import tech.amak.portbuddy.cli.config.ConfigurationService;
import tech.amak.portbuddy.cli.tunnel.HttpTunnelClient;
import tech.amak.portbuddy.cli.tunnel.NetTunnelClient;
import tech.amak.portbuddy.cli.ui.ConsoleUi;
import tech.amak.portbuddy.cli.utils.HttpUtils;
import tech.amak.portbuddy.common.ClientConfig;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.dto.ExposeRequest;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.auth.RegisterOtpRequest;
import tech.amak.portbuddy.common.dto.auth.RegisterRequest;
import tech.amak.portbuddy.common.dto.auth.RegisterResponse;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeRequest;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeResponse;

/**
 * Main class for the PortBuddy CLI application.
 * Handles command-line argument parsing and coordinates tunnel exposure.
 */
@Slf4j
public class PortBuddy {

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;
    private static final int EXIT_USAGE = 2;

    private final ConfigurationService configurationService = ConfigurationService.INSTANCE;

    private String domain;
    private String portReservation;
    private String passcode;
    private boolean verbose;
    private final List<String> positionalArgs = new ArrayList<>();

    private final OkHttpClient http = HttpUtils.createClient();

    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        final var portBuddy = new PortBuddy();
        final var exitCode = portBuddy.execute(args);
        System.exit(exitCode);
    }

    /**
     * Parses arguments and executes the requested command.
     *
     * @param args command line arguments
     * @return exit code
     */
    public int execute(final String[] args) {
        if (args.length == 0) {
            printHelp();
            return EXIT_USAGE;
        }

        var i = 0;
        while (i < args.length) {
            final var arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                printHelp();
                return EXIT_OK;
            } else if ("-V".equals(arg) || "--version".equals(arg)) {
                printVersion();
                return EXIT_OK;
            } else if ("-v".equals(arg) || "--verbose".equals(arg)) {
                this.verbose = true;
            } else if ("-d".equals(arg) || "--domain".equals(arg)) {
                if (++i < args.length) {
                    this.domain = args[i];
                } else {
                    System.err.println("Error: Option '-d', '--domain' requires an argument.");
                    return EXIT_USAGE;
                }
            } else if (arg.startsWith("--domain=")) {
                this.domain = arg.substring("--domain=".length());
            } else if ("-pr".equals(arg) || "--port-reservation".equals(arg)) {
                if (++i < args.length) {
                    this.portReservation = args[i];
                } else {
                    System.err.println("Error: Option '-pr', '--port-reservation' requires an argument.");
                    return EXIT_USAGE;
                }
            } else if (arg.startsWith("--port-reservation=")) {
                this.portReservation = arg.substring("--port-reservation=".length());
            } else if ("-pc".equals(arg) || "--passcode".equals(arg)) {
                if (++i < args.length) {
                    this.passcode = args[i];
                } else {
                    System.err.println("Error: Option '-pc', '--passcode' requires an argument.");
                    return EXIT_USAGE;
                }
            } else if (arg.startsWith("--passcode=")) {
                this.passcode = arg.substring("--passcode=".length());
            } else if ("-n".equals(arg) || "--no-request-log".equals(arg)) {
                configurationService.getConfig().setLogEnabled(false);
            } else if ("init".equals(arg)) {
                if (++i < args.length) {
                    return init(args[i]);
                } else {
                    System.err.println("Error: Missing API token for 'init' command.");
                    return EXIT_USAGE;
                }
            } else if (arg.startsWith("-")) {
                System.err.println("Unknown option: " + arg);
                printHelp();
                return EXIT_USAGE;
            } else {
                positionalArgs.add(arg);
            }
            i++;
        }

        return expose();
    }

    private void printHelp() {
        System.out.println("Usage: portbuddy [options] [mode] [host:][port]");
        System.out.println("Expose local ports to public network (simple ngrok alternative).");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -d, --domain=<domain>       Requested domain (e.g. my-domain)");
        System.out.println("  -pr, --port-reservation=<host:port>");
        System.out.println("                              Use specific port reservation host:port for TCP/UDP");
        System.out.println("  -pc, --passcode=<passcode>  Passcode to secure HTTP tunnel (temporary for this tunnel)");
        System.out.println("  -n, --no-request-log        Disable request logging");
        System.out.println("  -v, --verbose               Verbose logging");
        System.out.println("  -h, --help                  Show this help message and exit.");
        System.out.println("  -V, --version               Print version information and exit.");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  init <apiToken>             Initialize CLI with API token");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  portbuddy 3000");
        System.out.println("  portbuddy tcp 5432");
        System.out.println("  portbuddy --domain=my-app 8080");
    }

    private void printVersion() {
        System.out.println("portbuddy " + resolveCliVersion());
    }

    private int init(final String apiToken) {
        try {
            configurationService.saveApiToken(apiToken);
            System.out.println("API token saved. You're now authenticated.");
            return EXIT_OK;
        } catch (final IOException e) {
            System.err.println("Failed to save API token: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    private int expose() {
        final String modeStr;
        final String hostPortStr;
        if (positionalArgs.isEmpty()) {
            System.err.println("Usage: portbuddy [mode] [host:][port] or [schema://]host[:port]");
            return EXIT_USAGE;
        } else if (positionalArgs.size() == 1) {
            modeStr = null; // default http
            hostPortStr = positionalArgs.get(0);
        } else {
            modeStr = positionalArgs.get(0);
            hostPortStr = positionalArgs.get(1);
        }

        final var mode = TunnelType.from(modeStr);
        final var hostPort = parseHostPort(hostPortStr);
        if (hostPort == null) {
            return EXIT_USAGE;
        }
        if (hostPort.port < 1 || hostPort.port > 65535) {
            System.err.println("Port must be in range [1, 65535]");
            return EXIT_USAGE;
        }

        final var config = configurationService.getConfig();

        // 1) Ensure API key is present and exchange it for a JWT at startup
        if (!ensureAuthenticated(config)) {
            return EXIT_ERROR;
        }

        final var apiKey = config.getApiToken();
        final var jwt = exchangeApiTokenForJwt(config.getServerUrl(), apiKey);
        if (jwt == null || jwt.isBlank()) {
            // exchangeApiTokenForJwt already printed a specific reason.
            return EXIT_ERROR;
        }

        if (mode == TunnelType.HTTP) {
            final var expose = callExposeTunnel(config.getServerUrl(), jwt,
                new ExposeRequest(mode, hostPort.scheme, hostPort.host, hostPort.port, domain, null, passcode));
            if (expose == null) {
                // callExposeTunnel already printed a specific reason.
                return EXIT_ERROR;
            }

            final var localInfo = String.format("%s://%s:%d", hostPort.scheme, hostPort.host, hostPort.port);
            final var publicInfo = expose.publicUrl();
            final var ui = new ConsoleUi(TunnelType.HTTP, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null) {
                System.err.println("Server did not return tunnelId");
                return EXIT_ERROR;
            }

            final var client = new HttpTunnelClient(
                config.getServerUrl(),
                tunnelId,
                hostPort.host,
                hostPort.port,
                hostPort.scheme,
                jwt,
                publicInfo,
                ui,
                verbose
            );

            final var thread = new Thread(client::runBlocking, "port-buddy-http-client");
            ui.setOnExit(client::close);
            thread.start();
            ui.start();
            ui.waitForExit();
            try {
                thread.join(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            final var scheme = mode == TunnelType.UDP ? "udp" : "tcp";
            final var expose = callExposeTunnel(config.getServerUrl(), jwt,
                new ExposeRequest(mode, scheme, hostPort.host, hostPort.port, null, portReservation, null));
            if (expose == null) {
                // callExposeTunnel already printed a specific reason.
                return EXIT_ERROR;
            }
            if (expose.publicHost() == null || expose.publicPort() == null) {
                System.err.println("Failed to create " + mode + " tunnel: server did not return a public endpoint.");
                return EXIT_ERROR;
            }
            final var localInfo = String.format("%s %s:%d", mode.name().toLowerCase(), hostPort.host, hostPort.port);
            final var publicInfo = String.format("%s:%d", expose.publicHost(), expose.publicPort());
            final var ui = new ConsoleUi(mode, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null) {
                System.err.println("Server did not return tunnelId");
                return EXIT_ERROR;
            }
            // Use configured API server URL for the WebSocket control channel, not the public TCP host
            final var serverUri = URI.create(config.getServerUrl());
            final var wsHost = serverUri.getHost();
            final var wsPort = serverUri.getPort() == -1
                ? ("https".equalsIgnoreCase(serverUri.getScheme()) ? 443 : 80)
                : serverUri.getPort();
            final var secure = "https".equalsIgnoreCase(serverUri.getScheme());
            final var tcpClient = new NetTunnelClient(
                wsHost,
                wsPort,
                secure,
                tunnelId,
                hostPort.host,
                hostPort.port,
                mode,
                expose.publicHost(),
                expose.publicPort(),
                jwt,
                ui,
                verbose);
            final var thread = new Thread(tcpClient::runBlocking, "port-buddy-net-client-" + mode.name().toLowerCase());
            ui.setOnExit(tcpClient::close);
            thread.start();
            ui.start();
            ui.waitForExit();
            try {
                thread.join(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\nThanks, bye!");

        return EXIT_OK;
    }

    private ExposeResponse callExposeTunnel(final String baseUrl, final String jwt, final ExposeRequest requestBody) {
        final var tunnelType = requestBody.tunnelType();

        try {
            final var url = baseUrl + "/api/expose/"
                            + (tunnelType == TunnelType.HTTP ? "http" : "net");

            final var json = MAPPER.writeValueAsString(requestBody);
            final var request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + jwt)
                .build();

            try (final var response = http.newCall(request).execute()) {
                final var bodyStr = response.body() == null ? null : response.body().string();
                if (!response.isSuccessful()) {
                    log.warn("Expose {} failed: {} {}", tunnelType, response.code(), response.message());
                    printExposeError(baseUrl, response.code(), bodyStr, tunnelType);
                    return null;
                }
                if (bodyStr == null) {
                    System.err.println("Failed to create " + tunnelType + " tunnel: empty response from server.");
                    return null;
                }
                return MAPPER.readValue(bodyStr, ExposeResponse.class);
            }
        } catch (final Exception e) {
            log.warn("Expose {} tunnel call error: {}", tunnelType, e.toString());
            System.err.println("Failed to contact server to create " + tunnelType + " tunnel. "
                               + "Please check your connection and try again.");
            return null;
        }
    }

    /**
     * Prints a user-facing message for a failed expose call, reusing the server-provided reason
     * (RFC 7807 {@code detail}) and adding actionable guidance based on the HTTP status.
     */
    private void printExposeError(final String baseUrl, final int status, final String body,
                                  final TunnelType tunnelType) {
        final var reason = extractErrorMessage(body);
        switch (status) {
            case 402 -> {
                System.err.println(reason != null ? reason
                    : "Your subscription does not allow this tunnel. Please check your billing information.");
                System.err.println("Manage your subscription: " + billingUrl(baseUrl));
            }
            case 403 -> System.err.println(reason != null ? reason
                : "Your account is blocked. Please contact support.");
            case 401 -> System.err.println("Authentication failed. Please re-initialize CLI with a valid API Key.\n"
                                           + "Example: portbuddy init {API_TOKEN}");
            default -> System.err.println(reason != null ? reason
                : "Failed to create " + tunnelType + " tunnel (server returned status " + status + ").");
        }
    }

    /**
     * Builds the billing/subscription management page URL from the configured server URL.
     */
    private static String billingUrl(final String serverUrl) {
        final var base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        return base + "/app/billing";
    }

    /**
     * Exchanges the provided API token for a JWT using the server's auth endpoint.
     * Returns the JWT string if successful, otherwise null.
     */
    private String exchangeApiTokenForJwt(final String baseUrl, final String apiToken) {
        try {
            final var url = baseUrl + "/api/auth/token-exchange";
            final var cliVersion = resolveCliVersion();
            final var payload = new TokenExchangeRequest(apiToken, cliVersion);
            final var json = MAPPER.writeValueAsString(payload);
            final var request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

            try (final var response = http.newCall(request).execute()) {
                final var bodyStr = response.body() == null ? null : response.body().string();
                if (!response.isSuccessful()) {
                    log.warn("Token exchange failed: {} {}", response.code(), response.message());
                    printAuthError(response.code(), bodyStr);
                    return null;
                }
                if (bodyStr == null) {
                    System.err.println("Failed to authenticate: empty response from server.");
                    return null;
                }
                final var resp = MAPPER.readValue(bodyStr, TokenExchangeResponse.class);
                final var accessToken = resp.getAccessToken() == null ? "" : resp.getAccessToken();
                final var tokenType = resp.getTokenType() == null ? "" : resp.getTokenType();
                if (!accessToken.isBlank() && (tokenType.isBlank() || "Bearer".equalsIgnoreCase(tokenType))) {
                    return accessToken;
                }
                return null;
            }
        } catch (final Exception e) {
            log.warn("Token exchange call error: {}", e.toString());
            System.err.println("Failed to reach the server for authentication. "
                               + "Please check your connection and try again.");
            return null;
        }
    }

    /**
     * Prints a user-facing message for a failed token-exchange (authentication) call, reusing the
     * server-provided reason where useful and adding guidance based on the HTTP status.
     */
    private void printAuthError(final int status, final String body) {
        final var reason = extractErrorMessage(body);
        switch (status) {
            case 426 -> System.err.println("""
                Your portbuddy CLI is outdated.
                Please upgrade to the latest version and try again.""");
            case 403 -> System.err.println(reason != null ? reason
                : "Your account is blocked. Please contact support.");
            default -> System.err.println("""
                Failed to authenticate with the provided API Key.
                CLI must be initialized with a valid API Key.
                Example: portbuddy init {API_TOKEN}""");
        }
    }

    private String resolveCliVersion() {
        final var pkg = PortBuddy.class.getPackage();
        final var impl = pkg == null ? null : pkg.getImplementationVersion();
        if (impl != null && !impl.isBlank()) {
            return impl.trim();
        }
        return "1.0";
    }

    private boolean ensureAuthenticated(final ClientConfig config) {
        final var apiKey = config.getApiToken();
        if (apiKey != null && !apiKey.isBlank()) {
            return true;
        }

        final var serverUrl = config.getServerUrl();
        if (serverUrl == null || serverUrl.isBlank()) {
            System.err.println("Error: Server URL is not configured. Please check your application.yml.");
            return false;
        }

        try {
            final var action = ConsoleUi.promptInitialAction();
            if (action == ConsoleUi.InitialAction.INIT) {
                final var token = ConsoleUi.promptApiToken();
                configurationService.saveApiToken(token);
                config.setApiToken(token);
                System.out.println("Access token saved. You're now authenticated.");
                return true;
            }

            // Register a new account: email -> OTP -> password.
            final var email = ConsoleUi.promptEmail();
            if (!requestOtp(serverUrl, email)) {
                return false;
            }
            System.out.println("A 4-digit verification code has been sent to " + email + ".");
            final var otp = ConsoleUi.promptOtp();
            final var password = ConsoleUi.promptPassword();

            final var request = new RegisterRequest(email, null, password, otp);
            final var newApiKey = registerUser(serverUrl, request);
            configurationService.saveApiToken(newApiKey);
            config.setApiToken(newApiKey);
            System.out.println("Registration successful! API key saved.");
            return true;
        } catch (final Exception e) {
            log.debug("Authentication setup failed", e);
            System.err.println("Authentication setup failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Requests a one-time registration code to be emailed to the given address.
     * Returns true on success; on failure prints any server-provided reason and returns false.
     */
    private boolean requestOtp(final String baseUrl, final String email) {
        try {
            final var url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/auth/register/request-otp";
            final var json = MAPPER.writeValueAsString(new RegisterOtpRequest(email));
            final var request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

            try (final var response = http.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return true;
                }
                log.warn("Request OTP failed: {} {}", response.code(), response.message());
                final var reason = extractErrorMessage(response.body() == null ? null : response.body().string());
                System.err.println(reason != null ? reason
                    : "Failed to send verification code. Please try again later.");
                return false;
            }
        } catch (final Exception e) {
            log.warn("Request OTP call error: {}", e.toString());
            System.err.println("Failed to send verification code. Please try again later.");
            return false;
        }
    }

    private String extractErrorMessage(final String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            final var node = MAPPER.readTree(body);
            // Spring ProblemDetail (RFC 7807) carries the reason in "detail"; other endpoints use "message".
            final var detail = node.get("detail");
            if (detail != null && !detail.asText().isBlank()) {
                return detail.asText();
            }
            final var message = node.get("message");
            if (message != null && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (final Exception ignore) {
            // not JSON; fall through
        }
        return null;
    }

    private String registerUser(final String baseUrl,
                                final RegisterRequest registerRequest) throws IOException {
        final var url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/auth/register";
        final var json = MAPPER.writeValueAsString(registerRequest);
        final var request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(json, MediaType.parse("application/json")))
            .build();

        try (final var response = http.newCall(request).execute()) {
            final var respBody = response.body();
            if (respBody == null) {
                if (!response.isSuccessful()) {
                    throw new IOException("Server returned: " + response.message());
                }
                throw new IOException("Empty response from server");
            }

            if (response.code() == 503) {
                throw new IOException("Server is unavailable. Please try again later.");
            }

            final var bodyStr = respBody.string();
            try {
                final var registerResponse = MAPPER.readValue(bodyStr, RegisterResponse.class);
                if (!registerResponse.isSuccess()) {
                    throw new IOException(registerResponse.getMessage());
                }
                return registerResponse.getApiKey();
            } catch (final IOException e) {
                if (!response.isSuccessful()) {
                    throw new IOException("Server returned: " + response.message());
                }
                throw e;
            }
        }
    }

    private HostPort parseHostPort(final String arg) {
        var scheme = "http"; // default scheme
        var host = "localhost"; // default host
        Integer port = null;

        if (arg == null || arg.isBlank()) {
            System.err.println("Missing [host:][port] or [schema://]host[:port]. Example: 'portbuddy 3000' or 'portbuddy https://localhost'");
            return null;
        }

        var url = arg.trim();

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Case 1: pure port number, e.g. "3000"
        if (!url.contains("://") && !url.contains(":")) {
            // try parse as number; if fails, treat as host without port
            try {
                port = Integer.parseInt(url);
                return new HostPort(host, port, scheme);
            } catch (final NumberFormatException ignore) {
                // not a pure number -> host only, keep going
                host = url;
                port = null;
            }
        }

        var schemeExplicit = false;

        // Case 2: URL with scheme: http(s)://host[:port]
        if (url.contains("://")) {
            final var parts = url.split("://", 2);
            final var givenScheme = parts[0].toLowerCase();
            if (!givenScheme.equals("http") && !givenScheme.equals("https")) {
                System.err.println("Unsupported schema: " + givenScheme + ". Only http or https are allowed.");
                return null;
            }
            scheme = givenScheme;
            schemeExplicit = true;
            final var rest = parts[1];
            if (rest.contains(":")) {
                final var hostPortPart = rest.split(":", 2);
                host = hostPortPart[0].isBlank() ? host : hostPortPart[0];
                try {
                    port = Integer.parseInt(hostPortPart[1]);
                } catch (final NumberFormatException e) {
                    System.err.println("Invalid port: " + hostPortPart[1]);
                    return null;
                }
            } else if (!rest.isBlank()) {
                host = rest;
            }
        } else if (port == null) {
            // Case 3: host[:port] (no scheme)
            if (url.contains(":")) {
                final var hostPortPart = url.split(":", 2);
                host = hostPortPart[0].isBlank() ? host : hostPortPart[0];
                try {
                    port = Integer.parseInt(hostPortPart[1]);
                } catch (final NumberFormatException e) {
                    System.err.println("Invalid port: " + hostPortPart[1]);
                    return null;
                }
            } else {
                host = url;
            }
        }

        if (port == null) {
            // Default port by scheme
            port = scheme.equals("https") ? 443 : 80;
        }

        // If scheme is not explicit and port is 443/80, infer scheme from common defaults
        if (!schemeExplicit) {
            if (port == 443) {
                scheme = "https";
            } else if (port == 80) {
                scheme = "http";
            }
        }

        return new HostPort(host, port, scheme);
    }

    private static final class HostPort {
        private final String host;
        private final int port;
        private final String scheme;

        private HostPort(final String host, final int port, final String scheme) {
            this.host = host;
            this.port = port;
            this.scheme = scheme;
        }
    }
}
