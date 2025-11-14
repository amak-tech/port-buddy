package tech.amak.portbuddy.tcpproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String publicHost
) {
}
