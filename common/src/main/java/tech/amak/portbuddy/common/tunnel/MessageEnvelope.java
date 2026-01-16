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
