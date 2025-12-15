/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

/**
 * Resolves DNS TXT records and verifies visibility of ACME DNS-01 tokens.
 */
public interface DnsResolverService {

    /**
     * Checks whether a TXT record with the expected value is visible at the given FQDN.
     * Implementations may query multiple public resolvers.
     *
     * @param fqdn fully-qualified domain name of the TXT record
     * @param expectedValue expected TXT value
     * @return true if visible, false otherwise
     */
    boolean isTxtRecordVisible(String fqdn, String expectedValue);
}
