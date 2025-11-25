/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/** Configuration for tunnels housekeeping. */
@Getter
@Setter
@Component("tunnelsProperties")
@ConfigurationProperties(prefix = "app.tunnels")
public class TunnelsProperties {

    /**
     * How long a tunnel may stay without heartbeats before it is considered stale and closed.
     * Defaults to 2 minutes.
     */
    private Duration heartbeatTimeout = Duration.ofMinutes(2);

    /**
     * How often the server checks for stale tunnels.
     * Defaults to 30 seconds.
     */
    private Duration checkInterval = Duration.ofSeconds(30);
}
