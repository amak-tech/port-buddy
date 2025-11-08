package tech.amak.portbuddy.server.security;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Placeholder for future JWT support. Currently we rely on session after OAuth2 login.
 */
@Service
public class JwtService {
    public String createToken(final Map<String, Object> claims, final String subject) {
        throw new UnsupportedOperationException("JWT not enabled yet");
    }
}
