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

package tech.amak.portbuddy.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import tech.amak.portbuddy.common.tunnel.HttpTunnelMessage;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.db.repo.DomainRepository;
import tech.amak.portbuddy.server.service.ApiTokenService;
import tech.amak.portbuddy.server.service.TunnelService;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry;
import tech.amak.portbuddy.server.tunnel.TunnelRegistry.Tunnel;

@WebMvcTest(IngressController.class)
@AutoConfigureMockMvc(addFilters = false)
class IngressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TunnelRegistry registry;

    @MockitoBean
    private AppProperties properties;

    @MockitoBean
    private DomainRepository domainRepository;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private TunnelService tunnelService;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        final var gateway = new AppProperties.Gateway(
            "http://localhost", "portbuddy.dev", "https://%s.portbuddy.dev",
            "http://localhost/404", "http://localhost/passcode", DataSize.ofKilobytes(1)
        );
        when(properties.gateway()).thenReturn(gateway);
    }

    @Test
    void forwardViaTunnel_shouldRejectLargeRequest() throws Exception {
        final var subdomain = "test";
        final var tunnelId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();

        final var mockTunnel = mock(Tunnel.class);
        when(mockTunnel.isOpen()).thenReturn(true);
        when(mockTunnel.tunnelId()).thenReturn(tunnelId);
        when(mockTunnel.accountId()).thenReturn(accountId);
        when(registry.getBySubdomain(subdomain)).thenReturn(mockTunnel);

        final var account = new AccountEntity();
        account.setId(accountId);
        account.setSubscriptionStatus("active");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Content-Length = 2KB (exceeds 1KB limit)
        final var largeBody = new byte[2048];

        mockMvc.perform(post("/_/" + subdomain + "/some-path")
                .content(largeBody)
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().is(413));
    }

    @Test
    void forwardViaTunnel_shouldAcceptSmallRequest() throws Exception {
        final var subdomain = "test";
        final var tunnelId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();

        final var mockTunnel = mock(Tunnel.class);
        when(mockTunnel.isOpen()).thenReturn(true);
        when(mockTunnel.tunnelId()).thenReturn(tunnelId);
        when(mockTunnel.accountId()).thenReturn(accountId);
        when(registry.getBySubdomain(subdomain)).thenReturn(mockTunnel);

        final var account = new AccountEntity();
        account.setId(accountId);
        account.setSubscriptionStatus("active");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        final var responseMsg = new HttpTunnelMessage();
        responseMsg.setStatus(200);
        when(registry.forwardRequest(anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(responseMsg));
        
        // Content-Length = 512B (within 1KB limit)
        final var smallBody = new byte[512];

        mockMvc.perform(post("/_/" + subdomain + "/some-path")
                .content(smallBody)
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk());
    }
}
