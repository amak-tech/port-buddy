package tech.amak.portbuddy.common.tunnel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Lightweight control envelope for tunnel health/keep-alive messages.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlMessage {

    @JsonProperty("kind")
    private final String kind = "CTRL";

    @JsonProperty("type")
    private Type type;

    /**
     * Optional unix timestamp millis to help with diagnostics.
     */
    @JsonProperty("ts")
    private Long ts;

    public enum Type {
        PING,
        PONG
    }
}
