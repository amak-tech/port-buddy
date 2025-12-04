/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service.user;

/**
 * Thrown when a user provisioning attempt is made without an email address.
 */
public class MissingEmailException extends RuntimeException {

    public MissingEmailException(final String message) {
        super(message);
    }
}
