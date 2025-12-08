/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common.tunnel;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Envelope for WebSocket tunneling over the existing control WebSocket between server and CLI.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsTunnelMessage {

    /**
     * Constant marker to distinguish from HTTP messages.
     */
    @JsonProperty("kind")
    private final String kind = "WS";

    /**
     * Correlates messages of the same WS connection.
     */
    @JsonProperty("connectionId")
    private String connectionId;

    /**
     * Optional request/response id alignment if needed.
     */
    @JsonProperty("id")
    private String id;

    @JsonProperty("wsType")
    private Type wsType;

    // For OPEN from server to client
    @JsonProperty("path")
    private String path;

    @JsonProperty("query")
    private String query;

    @JsonProperty("headers")
    private Map<String, String> headers;

    // Payload
    @JsonProperty("text")
    private String text;

    @JsonProperty("dataB64")
    private String dataB64;

    // Close details
    @JsonProperty("closeCode")
    private Integer closeCode;

    @JsonProperty("closeReason")
    private String closeReason;

    public enum Type {
        OPEN,
        OPEN_OK,
        TEXT,
        BINARY,
        CLOSE,
        ERROR,
        /**
         * Control message sent by Net Proxy after WebSocket is established to inform CLI
         * about the actual exposed public endpoint details (host/port).
         */
        EXPOSED
    }

    // Public endpoint details for EXPOSED message
    @JsonProperty("publicHost")
    private String publicHost;

    @JsonProperty("publicPort")
    private Integer publicPort;
}
