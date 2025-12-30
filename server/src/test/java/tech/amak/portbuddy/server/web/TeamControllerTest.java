/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.InvitationEntity;
import tech.amak.portbuddy.server.db.entity.Role;
import tech.amak.portbuddy.server.db.entity.UserEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.UserRepository;
import tech.amak.portbuddy.server.security.ApiTokenAuthFilter;
import tech.amak.portbuddy.server.security.JwtService;
import tech.amak.portbuddy.server.security.Oauth2SuccessHandler;
import tech.amak.portbuddy.server.service.TeamService;

@WebMvcTest(TeamController.class)
@ActiveProfiles("test")
public class TeamControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private TeamController teamController;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ApiTokenAuthFilter apiTokenAuthFilter;

    @MockitoBean
    private Oauth2SuccessHandler oauth2SuccessHandler;

    private UUID accountId;
    private UUID userId;
    private AccountEntity account;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController)
            .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(final MethodParameter parameter) {
                    return parameter.getParameterType().equals(Jwt.class);
                }

                @Override
                public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer,
                                              final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
                    return createJwt();
                }
            })
            .build();

        accountId = UUID.randomUUID();
        userId = UUID.randomUUID();

        account = new AccountEntity();
        account.setId(accountId);
        account.setName("Test Team");

        user = new UserEntity();
        user.setId(userId);
        user.setEmail("admin@example.com");
        user.setAccount(account);
        user.setRoles(Set.of(Role.ACCOUNT_ADMIN, Role.USER));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void getMembers_ShouldReturnList() throws Exception {
        when(teamService.getMembers(any())).thenReturn(List.of(user));

        mockMvc.perform(get("/api/team/members")
                .principal(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(createJwt())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("admin@example.com"));
    }

    @Test
    void inviteMember_ShouldCreateInvitation() throws Exception {
        final var invitation = new InvitationEntity();
        invitation.setId(UUID.randomUUID());
        invitation.setEmail("new@example.com");
        invitation.setInvitedBy(user);
        invitation.setCreatedAt(OffsetDateTime.now());
        invitation.setExpiresAt(OffsetDateTime.now().plusDays(7));

        when(teamService.inviteMember(any(), any(), eq("new@example.com"))).thenReturn(invitation);

        final var request = new TeamController.InviteRequest();
        request.setEmail("new@example.com");

        mockMvc.perform(post("/api/team/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(createJwt())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void cancelInvitation_ShouldCallService() throws Exception {
        final var invId = UUID.randomUUID();

        mockMvc.perform(delete("/api/team/invitations/" + invId)
                .principal(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(createJwt())))
            .andExpect(status().isOk());

        verify(teamService).cancelInvitation(any(), eq(invId));
    }

    private org.springframework.security.oauth2.jwt.Jwt createJwt() {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("accountId", accountId.toString())
            .claim("roles", List.of("ACCOUNT_ADMIN", "USER"))
            .build();
    }
}
