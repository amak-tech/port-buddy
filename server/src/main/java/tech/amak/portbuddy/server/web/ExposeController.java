package tech.amak.portbuddy.server.web;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;
import tech.amak.portbuddy.server.client.TcpProxyClient;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

@RestController
@RequestMapping(path = "/api/expose", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ExposeController {

    private final SecureRandom random = new SecureRandom();
    private final TunnelRegistry registry;
    private final AppProperties properties;
    private final TcpProxyClient tcpProxyClient;

    @PostMapping("/http")
    public ExposeResponse exposeHttp(final @RequestBody HttpExposeRequest request) {
        final var subdomain = randomSubdomain();
        final var tunnelId = UUID.randomUUID().toString();
        registry.createPending(subdomain, tunnelId);
        final var gateway = properties.gateway();
        final var publicUrl = "%s://%s.%s".formatted(gateway.schema(), subdomain, gateway.domain());
        final var source = "http://" + request.host() + ":" + request.port();
        return new ExposeResponse(source, publicUrl, null, null, tunnelId, subdomain);
    }

    @PostMapping("/tcp")
    public ExposeResponse exposeTcp(final @RequestBody HttpExposeRequest request) {
        final var tunnelId = UUID.randomUUID().toString();

        // Ask the selected tcp-proxy to allocate a public TCP port for this tunnelId
        try {
            final var exposeResponse = tcpProxyClient.exposePort(tunnelId);
            log.info("Expose TCP port response: {}", exposeResponse);
            return exposeResponse;
        } catch (final Exception e) {
            log.error("Failed to allocate public TCP port for tunnelId={}: {}", tunnelId, e.getMessage(), e);
        }

        throw new RuntimeException("Failed to allocate public TCP port for tunnelId=" + tunnelId);
    }

    private String randomSubdomain() {
        final var animals = new String[] {"falcon", "lynx", "orca", "otter", "swift", "sparrow", "tiger", "puma"};
        final var name = animals[random.nextInt(animals.length)];
        final var num = 1000 + random.nextInt(9000);
        return name + "-" + num;
    }
}
