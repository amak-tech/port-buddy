/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientConfig {
    @JsonProperty("apiToken")
    private String apiToken;

    @JsonProperty("serverUrl")
    private String serverUrl;

    @JsonProperty("logLinesCount")
    private int logLinesCount = 20;

    @JsonProperty("logEnabled")
    private boolean logEnabled = false;

    @JsonProperty("consoleFrameDelay")
    private int consoleFrameDelayMs = 200;

    @JsonProperty("consoleFrameDelay")
    private int healthcheckIntervalSec = 5;
}
