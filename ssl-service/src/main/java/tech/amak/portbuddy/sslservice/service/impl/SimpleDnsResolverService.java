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

package tech.amak.portbuddy.sslservice.service.impl;

import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingException;
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

    private static final List<String> DNS_PROVIDERS = List.of(
        "dns://1.1.1.1 dns://8.8.8.8",
        "dns:"
    );

    @Override
    public boolean isTxtRecordVisible(final String fqdn, final String expectedValue) {
        for (final var providerUrl : DNS_PROVIDERS) {
            DirContext ctx = null;
            try {
                final var env = new Hashtable<String, String>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                env.put(Context.PROVIDER_URL, providerUrl);
                env.put("com.sun.jndi.dns.timeout.initial", "2000");
                env.put("com.sun.jndi.dns.timeout.retries", "1");
                ctx = new InitialDirContext(env);
                final Attributes attrs = ctx.getAttributes(fqdn, new String[] {"TXT"});
                final Attribute txt = attrs.get("TXT");
                if (txt != null) {
                    final var values = txt.getAll();
                    while (values.hasMoreElements()) {
                        final var raw = String.valueOf(values.nextElement());
                        final var normalized = raw.replaceAll("^\"|\"$", "");
                        if (normalized.contains(expectedValue)) {
                            return true;
                        }
                    }
                }
            } catch (final Exception e) {
                log.debug("TXT lookup failed for {} via {}: {}", fqdn, providerUrl, e.getMessage());
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (final NamingException e) {
                        log.debug("Failed to close DNS context", e);
                    }
                }
            }
        }
        return false;
    }
}
