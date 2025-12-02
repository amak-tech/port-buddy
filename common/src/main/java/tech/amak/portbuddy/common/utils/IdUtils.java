/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common.utils;

import java.net.URI;
import java.util.UUID;

public class IdUtils {

    private IdUtils() {
    }

    /**
     * Extracts the tunnel ID from the given URI by taking the last part of the URI path
     * and parsing it as a UUID. If the URI is null, its path is null, or the last part
     * of the path is not a valid UUID, the method will return null.
     *
     * @param uri the URI from which the tunnel ID is to be extracted
     * @return the UUID extracted from the last part of the URI path, or null if the input is invalid
     */
    public static UUID extractTunnelId(final URI uri) {
        if (uri == null) {
            return null;
        }
        final var path = uri.getPath();
        if (path == null) {
            return null;
        }
        final var parts = path.split("/");
        final var idPart = parts.length > 0 ? parts[parts.length - 1] : null;
        return idPart == null
            ? null
            : parseUuid(idPart);
    }

    /**
     * Parses a given string into a UUID. If the string cannot be parsed as a valid UUID,
     * the method returns null.
     *
     * @param id the string representation of the UUID to be parsed
     * @return the parsed UUID if the input is valid, or null if the input is invalid
     */
    public static UUID parseUuid(final String id) {
        try {
            return UUID.fromString(id);
        } catch (final Exception e) {
            return null;
        }
    }
}
