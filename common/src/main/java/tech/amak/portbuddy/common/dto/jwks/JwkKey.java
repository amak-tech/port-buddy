package tech.amak.portbuddy.common.dto.jwks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwkKey {

    @JsonProperty("kty")
    private String kty;

    @JsonProperty("kid")
    private String kid;

    @JsonProperty("use")
    private String use;

    @JsonProperty("alg")
    private String alg;

    // RSA specific
    @JsonProperty("n")
    private String n;

    @JsonProperty("e")
    private String e;
}
