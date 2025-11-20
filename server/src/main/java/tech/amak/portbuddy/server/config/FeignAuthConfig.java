package tech.amak.portbuddy.server.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@Configuration
public class FeignAuthConfig {

//    @Bean
//    public RequestInterceptor authorizationHeaderForwarder() {
//        return template -> {
//            final var attrs = RequestContextHolder.getRequestAttributes();
//            if (attrs instanceof ServletRequestAttributes servletAttrs) {
//                final var auth = servletAttrs.getRequest().getHeader(AUTHORIZATION);
//                if (auth != null && !auth.isBlank()) {
//                    template.header(AUTHORIZATION, auth);
//                }
//            }
//        };
//    }
}
