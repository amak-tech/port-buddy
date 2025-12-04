/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

public record PasswordResetConfirm(String token, String newPassword) {
}
