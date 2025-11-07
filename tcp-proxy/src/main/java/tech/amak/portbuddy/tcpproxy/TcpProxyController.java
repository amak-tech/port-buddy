package tech.amak.portbuddy.tcpproxy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/proxy", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TcpProxyController {

    private final TcpTunnelRegistry registry;

    @GetMapping("/expose")
    public ExposeResponse expose(final @RequestParam("tunnelId") String tunnelId) throws Exception {
        final var exposedPort = registry.expose(tunnelId);
        return new ExposeResponse(exposedPort.getPort());
    }

    public record ExposeResponse(int port) {
    }
}
