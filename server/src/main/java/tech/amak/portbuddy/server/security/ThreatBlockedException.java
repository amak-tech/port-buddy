package tech.amak.portbuddy.server.security;

public class ThreatBlockedException extends SecurityException {

    public ThreatBlockedException(final String message) {
        super(message);
    }
}
