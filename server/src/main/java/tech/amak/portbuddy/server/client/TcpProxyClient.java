package tech.amak.portbuddy.server.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import tech.amak.portbuddy.common.dto.ExposeResponse;

@FeignClient(
    name = "tcp-proxy",
    configuration = TcpProxyClient.Configuration.class
)
public interface TcpProxyClient {

    @PostMapping("/api/tcp-proxy/expose")
    ExposeResponse exposePort(@RequestParam("tunnelId") String tunnelId);

    class Configuration {

        @Bean
        public RequestInterceptor authorizationHeaderForwarder() {
            return template -> {
                final var attributes = RequestContextHolder.getRequestAttributes();
                if (attributes instanceof ServletRequestAttributes requestAttributes) {
                    final var auth = requestAttributes.getRequest().getHeader(AUTHORIZATION);
                    if (auth != null && !auth.isBlank()) {
                        template.header(AUTHORIZATION, auth);
                    }
                }
            };
        }
    }
}
