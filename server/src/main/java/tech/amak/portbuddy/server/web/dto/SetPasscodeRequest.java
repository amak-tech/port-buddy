/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web.dto;

/**
 * Request payload for setting or updating a passcode on a domain.
 *
 * @param passcode the raw passcode value to set for the domain
 */
public record SetPasscodeRequest(String passcode) {
}
