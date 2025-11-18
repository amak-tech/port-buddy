package tech.amak.portbuddy.server.web;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthApiController {

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal final Object principal) {
        if (principal instanceof Jwt jwt) {
            final var dto = new UserDto();
            dto.setId(jwt.getSubject());
            dto.setEmail(jwt.getClaimAsString("email"));
            dto.setName(jwt.getClaimAsString("name"));
            dto.setAvatarUrl(jwt.getClaimAsString("picture"));
            dto.setPlan(null);
            return dto;
        }
        if (principal instanceof DefaultOidcUser oidc) {
            final var dto = new UserDto();
            dto.setId(oidc.getSubject());
            dto.setEmail(oidc.getEmail());
            dto.setName(oidc.getFullName());
            dto.setAvatarUrl((String) oidc.getClaims().getOrDefault("picture", null));
            dto.setPlan(null);
            return dto;
        } else if (principal instanceof OAuth2User oauth2User) {
            final var attrs = oauth2User.getAttributes();
            final var dto = new UserDto();
            dto.setId(String.valueOf(attrs.getOrDefault("sub", attrs.getOrDefault("id", "unknown"))));
            dto.setEmail(String.valueOf(attrs.getOrDefault("email", "")));
            dto.setName(String.valueOf(attrs.getOrDefault("name", "")));
            dto.setAvatarUrl(String.valueOf(attrs.getOrDefault("picture", "")));
            dto.setPlan(null);
            return dto;
        }
        throw new RuntimeException("Unauthenticated");
    }

    @Data
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String avatarUrl;
        private String plan;
    }
}
