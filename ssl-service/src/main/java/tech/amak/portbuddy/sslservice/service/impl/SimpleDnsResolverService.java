/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service.impl;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.service.DnsResolverService;

/**
 * Simple DNS resolver that checks TXT record visibility using JNDI DNS provider.
 */
@Service
@Slf4j
public class SimpleDnsResolverService implements DnsResolverService {

    @Override
    public boolean isTxtRecordVisible(final String fqdn, final String expectedValue) {
        try {
            final var env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            final DirContext ctx = new InitialDirContext(env);
            final Attributes attrs = ctx.getAttributes(fqdn, new String[] {"TXT"});
            final Attribute txt = attrs.get("TXT");
            if (txt == null) {
                return false;
            }
            final var values = txt.getAll();
            while (values.hasMoreElements()) {
                final var raw = String.valueOf(values.nextElement());
                final var normalized = raw.replaceAll("^\"|\"$", "");
                if (normalized.contains(expectedValue)) {
                    return true;
                }
            }
            return false;
        } catch (final Exception e) {
            log.debug("TXT lookup failed for {}: {}", fqdn, e.getMessage());
            return false;
        }
    }
}
