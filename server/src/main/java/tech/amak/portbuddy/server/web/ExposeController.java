package tech.amak.portbuddy.server.web;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.common.dto.ExposeResponse;
import tech.amak.portbuddy.common.dto.HttpExposeRequest;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;

@RestController
@RequestMapping(path = "/api/expose", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class ExposeController {

    private final SecureRandom random = new SecureRandom();
    private final TunnelRegistry registry;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/http")
    public ExposeResponse exposeHttp(final @RequestBody HttpExposeRequest request) {
        final var subdomain = randomSubdomain();
        final var tunnelId = UUID.randomUUID().toString();
        registry.createPending(subdomain, tunnelId);
        final var publicUrl = "https://" + subdomain + ".port-buddy.com";
        final var source = "http://" + request.host() + ":" + request.port();
        return new ExposeResponse(source, publicUrl, null, null, tunnelId, subdomain);
    }

    @PostMapping("/tcp")
    public ExposeResponse exposeTcp(final @RequestBody HttpExposeRequest request) {
        final var proxyIdx = 1 + random.nextInt(10); // TODO: select least-loaded proxy
        final var publicHost = "tcp-proxy-" + proxyIdx + ".port-buddy.com";
        final var tunnelId = UUID.randomUUID().toString();
        final var source = "tcp " + request.host() + ":" + request.port();

        // Ask the selected tcp-proxy to allocate a public TCP port for this tunnelId
        try {
            final var url = "http://" + publicHost + "/api/proxy/expose?tunnelId=" + tunnelId;
            final var resp = restTemplate.getForObject(url, TcpExposeResp.class);
            if (resp != null) {
                return new ExposeResponse(source, null, publicHost, resp.port(), tunnelId, null);
            }
        } catch (final Exception e) {
            log.error("Failed to allocate public TCP port for tunnelId={}: {}", tunnelId, e.getMessage(), e);
        }

        throw new RuntimeException("Failed to allocate public TCP port for tunnelId=" + tunnelId);
    }

    private record TcpExposeResp(int port) {
    }

    private String randomSubdomain() {
        final var animals = new String[] {"falcon", "lynx", "orca", "otter", "swift", "sparrow", "tiger", "puma"};
        final var name = animals[random.nextInt(animals.length)];
        final var num = 1000 + random.nextInt(9000);
        return name + "-" + num;
    }
}
