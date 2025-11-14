package tech.amak.portbuddy.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import tech.amak.portbuddy.cli.tunnel.HttpTunnelClient;
import tech.amak.portbuddy.cli.tunnel.TcpTunnelClient;
import tech.amak.portbuddy.cli.ui.ConsoleUi;
import tech.amak.portbuddy.common.ClientConfig;
import tech.amak.portbuddy.common.Mode;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;

@Slf4j
@Command(
    name = "port-buddy",
    description = "Expose local ports to public network (simple ngrok alternative).",
    mixinStandardHelpOptions = true,
    version = {"port-buddy 1.0"},
    subcommands = {PortBuddy.InitCommand.class}
)
public class PortBuddy implements Callable<Integer> {

    @Mixin
    private SharedOptions shared;

    @Parameters(
        arity = "0..2",
        description = "[mode] [host:][port] or [schema://]host[:port]. Examples: '3000', 'localhost', 'example.com:8080', 'https://example.com'"
    )
    private java.util.List<String> args = new java.util.ArrayList<>();

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

        if (mode == Mode.HTTP) {
            final var config = loadConfig();
            final var expose = callExposeHttp(config.getServerUrl(),
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
                config.getApiToken(),
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
            final var config = loadConfig();
            final var expose = callExposeTcp(config.getServerUrl(),
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
            final var token = config.getApiToken();
            // Assume proxy WS control endpoint is on default HTTP port 80 for the public host
            final var tcpClient = new TcpTunnelClient(expose.publicHost(), 80, tunnelId, hostPort.host, hostPort.port, token, ui);
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

    private ExposeResponse callExposeHttp(String baseUrl, HttpExposeRequest reqBody) {
        try {
            final var url = baseUrl + "/api/expose/http";
            final var json = mapper.writeValueAsString(reqBody);
            final var cfg = loadConfig();
            final var reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")));
            if (cfg.getApiToken() != null && !cfg.getApiToken().isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + cfg.getApiToken());
            }
            final var request = reqBuilder.build();

            try (final var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Expose HTTP failed: {} {}", response.code(), response.message());
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

    private ExposeResponse callExposeTcp(final String baseUrl, final HttpExposeRequest reqBody) {
        try {
            final var url = baseUrl + "/api/expose/tcp";
            final var json = mapper.writeValueAsString(reqBody);
            final var cfg = loadConfig();
            final var reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")));
            if (cfg.getApiToken() != null && !cfg.getApiToken().isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + cfg.getApiToken());
            }
            final var request = reqBuilder.build();

            try (final var response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Expose TCP failed: {} {}", response.code(), response.message());
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

    private ClientConfig loadConfig() {
        final var config = new ClientConfig();
        try {
            final var home = System.getProperty("user.home");
            final var file = Path.of(home, ".port-buddy", "config.json");
            if (Files.exists(file)) {
                final var loaded = mapper.readValue(file.toFile(), ClientConfig.class);
                if (loaded.getServerUrl() != null) {
                    config.setServerUrl(loaded.getServerUrl());
                }
                if (loaded.getApiToken() != null) {
                    config.setApiToken(loaded.getApiToken());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load config: {}", e.toString());
        }
        return config;
    }

    private HostPort parseHostPort(final String arg) {
        var scheme = "http"; // default scheme
        var schemeExplicit = false;
        var host = "localhost"; // default host
        Integer port = null;

        if (arg == null || arg.isBlank()) {
            System.err.println("Missing [host:][port] or [schema://]host[:port]. Example: 'port-buddy 3000' or 'port-buddy https://localhost'");
            throw new CommandLine.ParameterException(new CommandLine(this), "Missing host/port argument");
        }

        final var s = arg.trim();

        // Case 1: pure port number, e.g. "3000"
        if (!s.contains("://") && !s.contains(":")) {
            // try parse as number; if fails, treat as host without port
            try {
                port = Integer.parseInt(s);
                return new HostPort(host, port, scheme);
            } catch (final NumberFormatException ignore) {
                // not a pure number -> host only, keep going
                host = s;
                port = null;
            }
        }

        // Case 2: URL with scheme: http(s)://host[:port]
        if (s.contains("://")) {
            final var parts = s.split("://", 2);
            final var givenScheme = parts[0].toLowerCase();
            if (!givenScheme.equals("http") && !givenScheme.equals("https")) {
                throw new CommandLine.ParameterException(new CommandLine(this), "Unsupported schema: " + givenScheme + ". Only http or https are allowed.");
            }
            scheme = givenScheme;
            schemeExplicit = true;
            final var rest = parts[1];
            if (rest.contains(":")) {
                final var hp = rest.split(":", 2);
                host = hp[0].isBlank() ? host : hp[0];
                try {
                    port = Integer.parseInt(hp[1]);
                } catch (final NumberFormatException e) {
                    throw new CommandLine.ParameterException(new CommandLine(this), "Invalid port: " + hp[1]);
                }
            } else if (!rest.isBlank()) {
                host = rest;
            }
        } else if (port == null) {
            // Case 3: host[:port] (no scheme)
            if (s.contains(":")) {
                final var hp = s.split(":", 2);
                host = hp[0].isBlank() ? host : hp[0];
                try {
                    port = Integer.parseInt(hp[1]);
                } catch (final NumberFormatException e) {
                    throw new CommandLine.ParameterException(new CommandLine(this), "Invalid port: " + hp[1]);
                }
            } else {
                host = s;
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

        private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        @Override
        public Integer call() throws Exception {
            final var config = new ClientConfig();
            config.setApiToken(apiToken);
            saveConfig(config);
            System.out.println("API token saved. You're now authenticated.");
            return CommandLine.ExitCode.OK;
        }

        private void saveConfig(ClientConfig cfg) throws IOException {
            final var dir = configDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            final var file = dir.resolve("config.json");
            mapper.writeValue(file.toFile(), cfg);
        }

        private Path configDir() {
            final var home = System.getProperty("user.home");
            return Path.of(home, ".port-buddy");
        }
    }
}
