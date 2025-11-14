package tech.amak.portbuddy.common.dto;

/** Request to expose a local HTTP service. */
public record HttpExposeRequest(String scheme, String host, int port) {}
