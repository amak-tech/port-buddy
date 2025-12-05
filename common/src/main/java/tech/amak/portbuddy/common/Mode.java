/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.common;

/**
 * Supported expose modes.
 */
public enum Mode {
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
    public static Mode from(final String mode) {
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
