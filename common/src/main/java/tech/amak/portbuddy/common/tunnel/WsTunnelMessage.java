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
