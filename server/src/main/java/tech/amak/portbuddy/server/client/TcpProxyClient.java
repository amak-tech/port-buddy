package tech.amak.portbuddy.server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import tech.amak.portbuddy.common.dto.ExposeResponse;

@FeignClient("tcp-proxy")
public interface TcpProxyClient {

    @PostMapping("/api/proxy/expose")
    ExposeResponse exposePort(@RequestParam("tunnelId") String tunnelId);

}
