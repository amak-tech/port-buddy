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
}
