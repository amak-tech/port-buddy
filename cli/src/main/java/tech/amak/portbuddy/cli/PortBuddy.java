/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.cli;

import static tech.amak.portbuddy.cli.utils.JsonUtils.MAPPER;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.amak.portbuddy.cli.config.ConfigurationService;
import tech.amak.portbuddy.cli.tunnel.HttpTunnelClient;
import tech.amak.portbuddy.cli.tunnel.NetTunnelClient;
import tech.amak.portbuddy.cli.ui.ConsoleUi;
import tech.amak.portbuddy.common.ClientConfig;
import tech.amak.portbuddy.common.TunnelType;
import tech.amak.portbuddy.common.dto.ExposeRequest;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.auth.RegisterRequest;
import tech.amak.portbuddy.common.dto.auth.RegisterResponse;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeRequest;
import tech.amak.portbuddy.common.dto.auth.TokenExchangeResponse;

@Slf4j
@Command(
    name = "port-buddy",
    description = "Expose local ports to public network (simple ngrok alternative).",
    mixinStandardHelpOptions = true,
    version = {"port-buddy 1.0"},
    subcommands = {PortBuddy.InitCommand.class}
)
public class PortBuddy implements Callable<Integer> {

    private static final String OUTDATED = "outdated";

    private final ConfigurationService configurationService = ConfigurationService.INSTANCE;

    @Mixin
    private SharedOptions shared;

    @Option(names = {"-d", "--domain"}, description = "Requested domain (e.g. my-domain or my-domain.portbuddy.dev)")
    private String domain;

    @Option(names = {"-pr", "--port-reservation"},
        description = "Use specific port reservation host:port for TCP/UDP (e.g. tcp-proxy-1.portbuddy.dev:45432)")
    private String portReservation;

    @Parameters(
        arity = "0..2",
        description = "[mode] [host:][port] or [schema://]host[:port]. Examples: '3000', 'localhost', 'example.com:8080', 'https://example.com'"
    )
    private final List<String> args = new ArrayList<>();

    private final OkHttpClient http = new OkHttpClient();

    static void main(final String[] args) {
        final var exit = new CommandLine(new PortBuddy()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() throws Exception {
        // Default command: expose
        return expose();
    }

    private int expose() {
        final String modeStr;
        final String hostPortStr;
        if (args.isEmpty()) {
            System.err.println("Usage: port-buddy [mode] [host:][port] or [schema://]host[:port]");
            return CommandLine.ExitCode.USAGE;
        } else if (args.size() == 1) {
            modeStr = null; // default http
            hostPortStr = args.get(0);
        } else {
            modeStr = args.get(0);
            hostPortStr = args.get(1);
        }

        final var mode = TunnelType.from(modeStr);
        final var hostPort = parseHostPort(hostPortStr);
        if (hostPort.port < 1 || hostPort.port > 65535) {
            System.err.println("Port must be in range [1, 65535]");
            return CommandLine.ExitCode.USAGE;
        }


        final var config = configurationService.getConfig();

        // 1) Ensure API key is present and exchange it for a JWT at startup
        if (!ensureAuthenticated(config)) {
            return CommandLine.ExitCode.SOFTWARE;
        }

        final var apiKey = config.getApiToken();
        final var jwt = exchangeApiTokenForJwt(config.getServerUrl(), apiKey);
        if (Objects.equals(jwt, OUTDATED)) {
            System.err.println("""
                Your port-buddy CLI is outdated.
                Please upgrade to the latest version and try again.""");
            return CommandLine.ExitCode.SOFTWARE;
        }

        if (jwt == null || jwt.isBlank()) {
            System.err.println("""
                Failed to authenticate with the provided API Key.
                CLI must be initialized with a valid API Key.
                Example: port-buddy init {API_TOKEN}""");
            return CommandLine.ExitCode.SOFTWARE;
        }

        if (mode == TunnelType.HTTP) {
            final var expose = callExposeTunnel(config.getServerUrl(), jwt,
                new ExposeRequest(mode, hostPort.scheme, hostPort.host, hostPort.port, domain, null));
            if (expose == null) {
                System.err.println("Failed to contact server to create tunnel");
                return CommandLine.ExitCode.SOFTWARE;
            }

            final var localInfo = String.format("%s://%s:%d", hostPort.scheme, hostPort.host, hostPort.port);
            final var publicInfo = expose.publicUrl();
            final var ui = new ConsoleUi(TunnelType.HTTP, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null) {
                System.err.println("Server did not return tunnelId");
                return CommandLine.ExitCode.SOFTWARE;
            }

            final var client = new HttpTunnelClient(
                config.getServerUrl(),
                tunnelId,
                hostPort.host,
                hostPort.port,
                hostPort.scheme,
                jwt,
                publicInfo,
                ui
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
                new ExposeRequest(mode, scheme, hostPort.host, hostPort.port, null, portReservation));
            if (expose == null || expose.publicHost() == null || expose.publicPort() == null) {
                System.err.println("Failed to contact server to create " + mode + " tunnel");
                return CommandLine.ExitCode.SOFTWARE;
            }
            final var localInfo = String.format("%s %s:%d", mode.name().toLowerCase(), hostPort.host, hostPort.port);
            final var publicInfo = String.format("%s:%d", expose.publicHost(), expose.publicPort());
            final var ui = new ConsoleUi(mode, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null) {
                System.err.println("Server did not return tunnelId");
                return CommandLine.ExitCode.SOFTWARE;
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
                jwt,
                ui);
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

        return CommandLine.ExitCode.OK;
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
                if (!response.isSuccessful()) {
                    log.warn("Expose {} failed: {} {}", tunnelType, response.code(), response.message());
                    if (response.code() == 401) {
                        System.err.println("Authentication failed. Please re-initialize CLI with a valid API Key.\n"
                                           + "Example: port-buddy init {API_TOKEN}");
                    }
                    return null;
                }
                final var body = response.body();
                if (body == null) {
                    return null;
                }
                return MAPPER.readValue(body.string(), ExposeResponse.class);
            }
        } catch (final Exception e) {
            log.warn("Expose {} tunnel call error: {}", tunnelType, e.toString());
            return null;
        }
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
                if (!response.isSuccessful()) {
                    log.warn("Token exchange failed: {} {}", response.code(), response.message());
                    if (response.code() == 426) {
                        return OUTDATED;
                    }
                    return null;
                }
                final var body = response.body();
                if (body == null) {
                    return null;
                }
                final var resp = MAPPER.readValue(body.string(), TokenExchangeResponse.class);
                final var accessToken = resp.getAccessToken() == null ? "" : resp.getAccessToken();
                final var tokenType = resp.getTokenType() == null ? "" : resp.getTokenType();
                if (!accessToken.isBlank() && (tokenType.isBlank() || "Bearer".equalsIgnoreCase(tokenType))) {
                    return accessToken;
                }
                return null;
            }
        } catch (final Exception e) {
            log.warn("Token exchange call error: {}", e.toString());
            return null;
        }
    }

    private String resolveCliVersion() {
        final var pkg = PortBuddy.class.getPackage();
        final var impl = pkg == null ? null : pkg.getImplementationVersion();
        if (impl != null && !impl.isBlank()) {
            return impl.trim();
        }
        // Fallback to picocli @Command version string if manifest is missing
        // Expected format: "port-buddy X.Y[.Z]"
        final var cmdAnno = PortBuddy.class.getAnnotation(Command.class);
        if (cmdAnno != null && cmdAnno.version() != null && cmdAnno.version().length > 0) {
            final var v = cmdAnno.version()[0];
            final var idx = v.lastIndexOf(' ');
            if (idx > -1 && idx < v.length() - 1) {
                return v.substring(idx + 1).trim();
            }
            return v.trim();
        }
        return "dev";
    }

    private boolean ensureAuthenticated(final ClientConfig config) {
        final var apiKey = config.getApiToken();
        if (apiKey != null && !apiKey.isBlank()) {
            return true;
        }

        System.out.println("No API key found. Please sign up.");

        try {
            final var request = ConsoleUi.promptForUserRegistration();

            final var newApiKey = registerUser(config.getServerUrl(), request);
            configurationService.saveApiToken(newApiKey);
            config.setApiToken(newApiKey);
            System.out.println("Registration successful! API key saved.");
            return true;
        } catch (final Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
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
            System.err.println("Missing [host:][port] or [schema://]host[:port]. Example: 'port-buddy 3000' or 'port-buddy https://localhost'");
            throw new CommandLine.ParameterException(new CommandLine(this), "Missing host/port argument");
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
                throw new CommandLine.ParameterException(
                    new CommandLine(this), "Unsupported schema: " + givenScheme + ". Only http or https are allowed.");
            }
            scheme = givenScheme;
            schemeExplicit = true;
            final var rest = parts[1];
            if (rest.contains(":")) {
                final var hostPort = rest.split(":", 2);
                host = hostPort[0].isBlank() ? host : hostPort[0];
                try {
                    port = Integer.parseInt(hostPort[1]);
                } catch (final NumberFormatException e) {
                    throw new CommandLine.ParameterException(new CommandLine(this), "Invalid port: " + hostPort[1]);
                }
            } else if (!rest.isBlank()) {
                host = rest;
            }
        } else if (port == null) {
            // Case 3: host[:port] (no scheme)
            if (url.contains(":")) {
                final var hostPort = url.split(":", 2);
                host = hostPort[0].isBlank() ? host : hostPort[0];
                try {
                    port = Integer.parseInt(hostPort[1]);
                } catch (final NumberFormatException e) {
                    throw new CommandLine.ParameterException(new CommandLine(this), "Invalid port: " + hostPort[1]);
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

    static final class HostPort {
        final String host;
        final int port;
        final String scheme;

        HostPort(final String host, final int port, final String scheme) {
            this.host = host;
            this.port = port;
            this.scheme = scheme;
        }
    }

    static class SharedOptions {
        @Option(names = {"-v", "--verbose"}, description = "Verbose logging")
        boolean verbose;
    }

    @Command(name = "init", description = "Initialize CLI with API token")
    static class InitCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "API token from your account")
        private String apiToken;

        @Override
        public Integer call() throws Exception {
            ConfigurationService.INSTANCE.saveApiToken(apiToken);
            System.out.println("API token saved. You're now authenticated.");
            return CommandLine.ExitCode.OK;
        }

    }
}
