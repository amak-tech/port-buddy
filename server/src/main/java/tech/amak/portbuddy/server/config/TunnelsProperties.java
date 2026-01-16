/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
