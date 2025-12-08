/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.mail;

import java.util.UUID;

/**
 * Application domain event published when a new user is created in the system.
 */
public record UserCreatedEvent(UUID userId,
                               UUID accountId,
                               String email,
                               String firstName,
                               String lastName,
                               String passwordResetLink) {
}
