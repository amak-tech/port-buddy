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

    @Parameters(arity = "0..2", description = "[mode] [host:][port] e.g. 'tcp 127.0.0.1:5432' or '3000'")
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
            System.err.println("Usage: port-buddy [mode] [host:][port]");
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
            final var expose = callExposeHttp(config.getServerUrl(), new HttpExposeRequest(hostPort.host, hostPort.port));
            if (expose == null) {
                System.err.println("Failed to contact server to create tunnel");
                return CommandLine.ExitCode.SOFTWARE;
            }

            final var localInfo = String.format("http://%s:%d", hostPort.host, hostPort.port);
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
                config.getApiToken(),
                publicInfo,
                ui
            );

            final var t = new Thread(client::runBlocking, "port-buddy-http-client");
            ui.setOnExit(client::close);
            t.start();
            ui.start();
            ui.waitForExit();
            try {
                t.join(2000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            final var config = loadConfig();
            final var expose = callExposeTcp(config.getServerUrl(), new HttpExposeRequest(hostPort.host, hostPort.port));
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
            final var t = new Thread(tcpClient::runBlocking, "port-buddy-tcp-client");
            ui.setOnExit(tcpClient::close);
            t.start();
            ui.start();
            ui.waitForExit();
            try {
                t.join(2000);
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

    private HostPort parseHostPort(String arg) {
        var host = "localhost";
        var portStr = arg;
        if (arg == null || arg.isBlank()) {
            System.err.println("Missing [host:][port]. Example: 'port-buddy 3000' or 'port-buddy tcp 127.0.0.1:5432'");
            throw new CommandLine.ParameterException(new CommandLine(this), "Missing host/port argument");
        }
        if (arg.contains(":")) {
            final var parts = arg.split(":", 2);
            host = parts[0].isBlank() ? host : parts[0];
            portStr = parts[1];
        }
        final var port = Integer.parseInt(portStr);
        return new HostPort(host, port);
    }

    static final class HostPort {
        final String host;
        final int port;

        HostPort(String host, int port) {
            this.host = host;
            this.port = port;
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
