package tech.amak.portbuddy.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    Gateway gateway
) {
    public record Gateway(
        String url,
        String domain,
        String schema
    ) {
        public String subdomainHost() {
            return "." + domain;
        }
    }
}
