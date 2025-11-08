package tech.amak.portbuddy.common.tunnel;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Envelope for HTTP tunnel messages exchanged over WebSocket between server and CLI.
 * To keep it simple, messages are whole-request/whole-response with base64 bodies.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpTunnelMessage {

    /**
     * Unique ID to correlate request and response.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Message type.
     */
    @JsonProperty("type")
    private Type type;

    // Request fields
    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("query")
    private String query;

    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Request body encoded as Base64.
     */
    @JsonProperty("bodyB64")
    private String bodyB64;

    // Response fields
    @JsonProperty("status")
    private Integer status;

    @JsonProperty("respHeaders")
    private Map<String, String> respHeaders;

    /**
     * Response body encoded as Base64.
     */
    @JsonProperty("respBodyB64")
    private String respBodyB64;

    public enum Type {
        REQUEST,
        RESPONSE
    }
}
