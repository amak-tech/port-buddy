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

package tech.amak.portbuddy.server.web.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.repo.AccountRepository;
import tech.amak.portbuddy.server.service.TunnelService;

@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TunnelService tunnelService;

    @InjectMocks
    private AdminAccountController adminAccountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAccountController).build();
    }

    @Test
    void blockAccount_shouldSetBlockedTrueAndSaveAndCloseTunnels() throws Exception {
        final var accountId = UUID.randomUUID();
        final var account = new AccountEntity();
        account.setId(accountId);
        account.setBlocked(false);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        mockMvc.perform(post("/api/admin/accounts/{accountId}/block", accountId))
            .andExpect(status().isNoContent());

        verify(accountRepository).save(account);
        verify(tunnelService).closeAllTunnels(account);
        assert (account.isBlocked());
    }
}
