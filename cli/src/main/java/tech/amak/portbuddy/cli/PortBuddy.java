package tech.amak.portbuddy.cli;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
import tech.amak.portbuddy.cli.tunnel.TcpTunnelClient;
import tech.amak.portbuddy.cli.ui.ConsoleUi;
import tech.amak.portbuddy.common.Mode;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;
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

    private final ConfigurationService configurationService = ConfigurationService.INSTANCE;

    @Mixin
    private SharedOptions shared;

    @Parameters(
        arity = "0..2",
        description = "[mode] [host:][port] or [schema://]host[:port]. Examples: '3000', 'localhost', 'example.com:8080', 'https://example.com'"
    )
    private List<String> args = new ArrayList<>();

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final OkHttpClient http = new OkHttpClient();

    static void main(String[] args) {
        var exit = new CommandLine(new PortBuddy()).execute(args);
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

        final var mode = Mode.from(modeStr);
        final var hostPort = parseHostPort(hostPortStr);
        if (hostPort.port < 1 || hostPort.port > 65535) {
            System.err.println("Port must be in range [1, 65535]");
            return CommandLine.ExitCode.USAGE;
        }


        final var config = configurationService.getConfig();

        // 1) Ensure API key is present and exchange it for a JWT at startup
        final var apiKey = config.getApiToken();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Authentication required. CLI must be initialized with a valid API Key.\n"
                               + "Run: port-buddy init {API_TOKEN}");
            return CommandLine.ExitCode.SOFTWARE;
        }

        final var jwt = exchangeApiTokenForJwt(config.getServerUrl(), apiKey);
        if (jwt == null || jwt.isBlank()) {
            System.err.println("Failed to authenticate with the provided API Key.\n"
                               + "CLI must be initialized with a valid API Key.\n"
                               + "Example: port-buddy init {API_TOKEN}");
            return CommandLine.ExitCode.SOFTWARE;
        }

        if (mode == Mode.HTTP) {
            final var expose = callExposeHttp(config.getServerUrl(), jwt,
                new HttpExposeRequest(hostPort.scheme, hostPort.host, hostPort.port));
            if (expose == null) {
                System.err.println("Failed to contact server to create tunnel");
                return CommandLine.ExitCode.SOFTWARE;
            }

            final var localInfo = String.format("%s://%s:%d", hostPort.scheme, hostPort.host, hostPort.port);
            final var publicInfo = expose.publicUrl();
            final var ui = new ConsoleUi(Mode.HTTP, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null || tunnelId.isBlank()) {
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
            final var expose = callExposeTcp(config.getServerUrl(), jwt,
                new HttpExposeRequest("tcp", hostPort.host, hostPort.port));
            if (expose == null || expose.publicHost() == null || expose.publicPort() == null) {
                System.err.println("Failed to contact server to create TCP tunnel");
                return CommandLine.ExitCode.SOFTWARE;
            }
            final var localInfo = String.format("tcp %s:%d", hostPort.host, hostPort.port);
            final var publicInfo = String.format("%s:%d", expose.publicHost(), expose.publicPort());
            final var ui = new ConsoleUi(Mode.TCP, localInfo, publicInfo);
            final var tunnelId = expose.tunnelId();
            if (tunnelId == null || tunnelId.isBlank()) {
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
            final var tcpClient = new TcpTunnelClient(wsHost, wsPort, secure, tunnelId, hostPort.host, hostPort.port, jwt, ui);
            final var thread = new Thread(tcpClient::runBlocking, "port-buddy-tcp-client");
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

    private ExposeResponse callExposeHttp(final String baseUrl, final String jwt, final HttpExposeRequest reqBody) {
        try {
            final var url = baseUrl + "/api/expose/http";
            final var json = mapper.writeValueAsString(reqBody);
            final var reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + jwt);
            final var request = reqBuilder.build();

            try (final var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Expose HTTP failed: {} {}", response.code(), response.message());
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
                final var str = body.string();
                return mapper.readValue(str, ExposeResponse.class);
            }
        } catch (Exception e) {
            log.warn("Expose HTTP call error: {}", e.toString());
            return null;
        }
    }

    private ExposeResponse callExposeTcp(final String baseUrl, final String jwt, final HttpExposeRequest reqBody) {
        try {
            final var url = baseUrl + "/api/expose/tcp";
            final var json = mapper.writeValueAsString(reqBody);
            final var reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + jwt);
            final var request = reqBuilder.build();

            try (final var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Expose TCP failed: {} {}", response.code(), response.message());
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
                return mapper.readValue(body.string(), ExposeResponse.class);
            }
        } catch (final Exception e) {
            log.warn("Expose TCP call error: {}", e.toString());
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
            final var payload = new TokenExchangeRequest(apiToken);
            final var json = mapper.writeValueAsString(payload);
            final var request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

            try (final var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Token exchange failed: {} {}", response.code(), response.message());
                    return null;
                }
                final var body = response.body();
                if (body == null) {
                    return null;
                }
                final var resp = mapper.readValue(body.string(), TokenExchangeResponse.class);
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
                throw new CommandLine.ParameterException(new CommandLine(this), "Unsupported schema: " + givenScheme + ". Only http or https are allowed.");
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
