package tech.amak.portbuddy.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response with public exposure details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExposeResponse(
    String source,
    String publicUrl,
    String publicHost,
    Integer publicPort,
    String tunnelId,
    String subdomain
) {
}
