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

package tech.amak.portbuddy.common;

/**
 * Supported expose modes.
 */
public enum TunnelType {
    HTTP,
    TCP,
    UDP;

    /**
     * Converts a string representation of a mode to its corresponding {@code Mode} enum value.
     * If the provided string is {@code null}, defaults to {@code HTTP}.
     *
     * @param mode the string representation of the mode, such as "http" or "tcp".
     *             Case-insensitive. If {@code null}, the method returns {@code HTTP}.
     * @return the corresponding {@code Mode} enum value.
     * @throws IllegalArgumentException if the string does not match any supported mode.
     */
    public static TunnelType from(final String mode) {
        if (mode == null) {
            return HTTP;
        }
        return switch (mode.toLowerCase()) {
            case "http" -> HTTP;
            case "tcp" -> TCP;
            case "udp" -> UDP;
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
