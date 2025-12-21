/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tech.amak.portbuddy.server.client.SslServiceClient;
import tech.amak.portbuddy.server.config.AppProperties;
import tech.amak.portbuddy.server.db.entity.AccountEntity;
import tech.amak.portbuddy.server.db.entity.DomainEntity;
import tech.amak.portbuddy.server.db.entity.TunnelStatus;
import tech.amak.portbuddy.server.db.repo.DomainRepository;
import tech.amak.portbuddy.server.db.repo.TunnelRepository;

@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    @Mock
    private DomainRepository domainRepository;
    @Mock
    private TunnelRepository tunnelRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SslServiceClient sslServiceClient;

    private DomainService domainService;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        final var gateway = new AppProperties.Gateway(
            "url",
            "portbuddy.dev",
            "https",
            "https://portbuddy.dev/404",
            "https://portbuddy.dev/passcode");
        final var mail = new AppProperties.Mail("no-reply@localhost", "Port Buddy");
        final var portReservations =
            new AppProperties.PortReservations(new AppProperties.PortReservations.Range(40000, 60000));
        final var appProps = new AppProperties(
            gateway,
            null,
            null,
            mail,
            new AppProperties.Cli("1.0"),
            portReservations);

        domainService = new DomainService(domainRepository, tunnelRepository, appProps, passwordEncoder, sslServiceClient);
        account = new AccountEntity();
        account.setId(UUID.randomUUID());
    }

    @Test
    void createDomain_Success() {
        when(domainRepository.existsBySubdomainGlobal(anyString())).thenReturn(false);
        when(domainRepository.save(any(DomainEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        final var domainOpt = domainService.createDomain(account);

        assertTrue(domainOpt.isPresent());
        final var domain = domainOpt.get();
        assertEquals("portbuddy.dev", domain.getDomain());
        assertNotNull(domain.getSubdomain());
        verify(domainRepository).save(any(DomainEntity.class));
    }

    @Test
    void createDomain_RetryOnCollision() {
        // First attempt collision, second success
        when(domainRepository.existsBySubdomainGlobal(anyString()))
            .thenReturn(true)
            .thenReturn(false);
        when(domainRepository.save(any(DomainEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        final var domain = domainService.createDomain(account);

        assertTrue(domain.isPresent());
        // Called 3 times: 1st loop (collision), 2nd loop (success), post-loop check (success)
        verify(domainRepository, times(3)).existsBySubdomainGlobal(anyString());
    }

    @Test
    void updateDomain_Success() {
        final var id = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setId(id);
        domain.setSubdomain("old");
        domain.setAccount(account);

        when(domainRepository.findByIdAndAccount(id, account)).thenReturn(Optional.of(domain));
        when(tunnelRepository.existsByDomainAndStatusNot(domain, TunnelStatus.CLOSED)).thenReturn(false);
        when(domainRepository.existsBySubdomainGlobal("new")).thenReturn(false);
        when(domainRepository.save(any(DomainEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        final var updated = domainService.updateDomain(id, account, "new");

        assertEquals("new", updated.getSubdomain());
        verify(domainRepository).save(domain);
    }

    @Test
    void updateDomain_ActiveTunnel() {
        final var id = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setId(id);
        domain.setSubdomain("old");

        when(domainRepository.findByIdAndAccount(id, account)).thenReturn(Optional.of(domain));
        when(tunnelRepository.existsByDomainAndStatusNot(domain, TunnelStatus.CLOSED)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> domainService.updateDomain(id, account, "new"));
    }

    @Test
    void deleteDomain_Success() {
        final var id = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setId(id);
        domain.setSubdomain("foo");

        when(domainRepository.findByIdAndAccount(id, account)).thenReturn(Optional.of(domain));
        when(tunnelRepository.existsByDomainAndStatusNot(domain, TunnelStatus.CLOSED)).thenReturn(false);

        domainService.deleteDomain(id, account);

        verify(domainRepository).delete(domain);
    }

    @Test
    void deleteCustomDomain_Success() {
        final var id = UUID.randomUUID();
        final var domain = new DomainEntity();
        domain.setId(id);
        domain.setCustomDomain("custom.com");
        domain.setCnameVerified(true);
        domain.setAccount(account);

        when(domainRepository.findByIdAndAccount(id, account)).thenReturn(Optional.of(domain));

        domainService.deleteCustomDomain(id, account);

        assertEquals(null, domain.getCustomDomain());
        assertEquals(false, domain.isCnameVerified());
        verify(domainRepository).save(domain);
    }
}
