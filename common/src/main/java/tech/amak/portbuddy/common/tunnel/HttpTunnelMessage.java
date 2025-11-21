package tech.amak.portbuddy.common.tunnel;

import java.util.List;
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
    private Map<String, List<String>> headers;

    /**
     * Request body encoded as Base64.
     */
    @JsonProperty("bodyB64")
    private String bodyB64;

    /**
     * Original request body media type (e.g., "application/json; charset=utf-8").
     * The server captures this from the ingress request and the CLI uses it to
     * reconstruct the upstream request body with the same Content-Type.
     */
    @JsonProperty("bodyContentType")
    private String bodyContentType;

    // Response fields
    @JsonProperty("status")
    private Integer status;

    @JsonProperty("respHeaders")
    private Map<String, List<String>> respHeaders;

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
