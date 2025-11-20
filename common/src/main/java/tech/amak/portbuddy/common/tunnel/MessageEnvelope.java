package tech.amak.portbuddy.common.tunnel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Minimal envelope to route incoming WS messages without using JsonNode.
 * If {@code kind} is null, treat it as an HTTP tunnel message.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageEnvelope {

    @JsonProperty("kind")
    private String kind; // CTRL, WS or null (HTTP)
}
