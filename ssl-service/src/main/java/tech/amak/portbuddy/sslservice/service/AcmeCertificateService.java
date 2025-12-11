/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.domain.CertificateEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateJobEntity;
import tech.amak.portbuddy.sslservice.domain.CertificateJobStatus;
import tech.amak.portbuddy.sslservice.domain.CertificateStatus;
import tech.amak.portbuddy.sslservice.repo.CertificateJobRepository;
import tech.amak.portbuddy.sslservice.repo.CertificateRepository;
import tech.amak.portbuddy.sslservice.work.ChallengeTokenStore;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcmeCertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateJobRepository jobRepository;
    private final ChallengeTokenStore challengeTokenStore;
    private final AcmeAccountService acmeAccountService;
    private final AcmeClientService acmeClientService;
    private final CertificateStorageService storageService;
    private final RetryExecutor retryExecutor;

    /**
     * Submits an asynchronous job to issue or renew a certificate for the given domain.
     *
     * @param domain      the domain to issue or renew certificate for
     * @param requestedBy username of requester
     * @return persisted job entity
     */
    @Transactional
    public CertificateJobEntity submitJob(final String domain, final String requestedBy) {
        final var normalizedDomain = domain.toLowerCase();

        // Prevent duplicate jobs for the same domain when a job is already pending or running
        final var activeStatuses = Set.of(CertificateJobStatus.PENDING, CertificateJobStatus.RUNNING);
        final var existsActive = jobRepository.existsByDomainIgnoreCaseAndStatusIn(normalizedDomain, activeStatuses);
        if (existsActive) {
            throw new IllegalStateException("A certificate job is already in progress for domain: " + normalizedDomain);
        }

        final var job = new CertificateJobEntity();
        job.setDomain(normalizedDomain);
        job.setStatus(CertificateJobStatus.PENDING);
        final var savedJob = jobRepository.save(job);
        // Fire and forget async processing
        processJobAsync(savedJob.getId());
        return savedJob;
    }

    /**
     * Processes the job asynchronously. This method encapsulates ACME/Let’s Encrypt logic.
     * In this initial implementation, it simulates success and updates DB records.
     *
     * @param jobId the job identifier
     */
    @Async
    @Transactional
    public void processJobAsync(final UUID jobId) {
        final var job = jobRepository.findById(jobId).orElseThrow();
        MDC.put("jobId", String.valueOf(jobId));
        MDC.put("domain", job.getDomain());
        job.setStatus(CertificateJobStatus.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        jobRepository.save(job);

        try {
            performAcmeHttp01Issuance(job);
        } catch (final Exception e) {
            log.error("Certificate job failed", e);
            job.setStatus(CertificateJobStatus.FAILED);
            job.setFinishedAt(OffsetDateTime.now());
            job.setMessage(e.getMessage());
            jobRepository.save(job);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Performs the full ACME HTTP-01 issuance flow for the job's domain.
     *
     * @param job the job to process
     */
    private void performAcmeHttp01Issuance(final CertificateJobEntity job) throws Exception {
        final var domain = job.getDomain();

        updateJobMessage(job, "Starting issuance for '%s'", domain);

        // 1) Create session and load/login account
        final Session session = acmeClientService.newSession();
        updateJobMessage(job, "ACME session created");

        final KeyPair accountKeyPair = acmeAccountService.loadAccountKeyPair();
        updateJobMessage(job, "Account key loaded");

        final Account account = retryExecutor.callWithRetry("acme.login", () -> {
            final var acc = acmeClientService.loginOrRegister(session, accountKeyPair);
            return acc;
        });
        updateJobMessage(job, "Logged into ACME account");

        // 2) Create a new order for the domain
        final Order order = retryExecutor.callWithRetry("acme.order.create", () -> account
            .newOrder().domains(domain).create());
        updateJobMessage(job, "ACME order created");

        // 3) Complete HTTP-01 challenge for each authorization
        for (final Authorization auth : order.getAuthorizations()) {
            if (auth.getStatus() == org.shredzone.acme4j.Status.VALID) {
                continue;
            }
            final Http01Challenge httpChallenge = auth.getChallenges().stream()
                .filter(c -> c.getType().equals(Http01Challenge.TYPE))
                .map(c -> (Http01Challenge) c)
                .findFirst()
                .orElse(null);
            if (httpChallenge == null) {
                throw new IllegalStateException("HTTP-01 challenge not available for domain: " + domain);
            }
            final var token = httpChallenge.getToken();
            final var challengeContent = httpChallenge.getAuthorization();
            challengeTokenStore.putToken(token, challengeContent);
            try {
                updateJobMessage(job, "HTTP-01 challenge published (token=%s)", token);
                retryExecutor.callWithRetry("acme.challenge.trigger", () -> {
                    httpChallenge.trigger();
                    return Boolean.TRUE;
                });
                // poll until VALID or failure with backoff
                pollAuthorizationValidWithRetry(auth, 90, 2_000);
                updateJobMessage(job, "HTTP-01 challenge validated");
            } finally {
                challengeTokenStore.removeToken(token);
            }
        }

        // 4) Generate domain key and CSR
        final KeyPair domainKeyPair = storageService.generateRsaKeyPair();
        final var csr = buildCsrDer(domain, domainKeyPair);
        updateJobMessage(job, "CSR generated");

        // 5) Finalize order
        retryExecutor.callWithRetry("acme.order.finalize", () -> {
            order.execute(csr);
            return Boolean.TRUE;
        });
        updateJobMessage(job, "Order finalized, waiting for issuance");
        pollOrderValidWithRetry(order, 120, 2_000);

        // 6) Download certificate: acme4j returns X509Certificate(s); convert to PEM strings
        final var downloaded = retryExecutor.callWithRetry("acme.cert.download", order::getCertificate);
        final var certChain = downloaded.getCertificateChain();

        // Convert primary certificate to PEM string
        final var leafCertPem = toPem(downloaded.getCertificate());
        final var chainPem = certChain == null
            ? ""
            : certChain.stream()
            .skip(1)
            .map(this::toPem)
            .reduce("", (a, b) -> a + b);

        // 7) Store files
        final var keyPath = storageService.writePrivateKeyPem(domain, domainKeyPair);
        final var certPath = storageService.writeCertPem(domain, leafCertPem);
        final var chainPath = storageService.writeChainPem(domain, chainPem);

        // 8) Update DB
        var certificate = certificateRepository.findByDomain(domain).orElse(null);
        if (certificate == null) {
            certificate = new CertificateEntity();
            certificate.setDomain(domain);
        }

        // Try to extract validity from leaf certificate
        final var x509 = downloaded.getCertificate();
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setIssuedAt(OffsetDateTime.ofInstant(x509.getNotBefore().toInstant(), java.time.ZoneOffset.UTC));
        certificate.setExpiresAt(OffsetDateTime.ofInstant(x509.getNotAfter().toInstant(), java.time.ZoneOffset.UTC));
        certificate.setPrivateKeyPath(keyPath.toAbsolutePath().toString());
        certificate.setCertificatePath(certPath.toAbsolutePath().toString());
        certificate.setChainPath(chainPath.toAbsolutePath().toString());
        certificateRepository.save(certificate);

        job.setStatus(CertificateJobStatus.SUCCEEDED);
        job.setFinishedAt(OffsetDateTime.now());
        job.setMessage("Certificate issued/renewed successfully.");
        jobRepository.save(job);
    }

    private void pollAuthorizationValid(final Authorization auth, final int maxSeconds, final long sleepMillis)
        throws InterruptedException, AcmeException {
        final long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            auth.update();
            final var status = auth.getStatus();
            if (status == org.shredzone.acme4j.Status.VALID) {
                return;
            }
            if (status == org.shredzone.acme4j.Status.INVALID) {
                throw new IllegalStateException("Authorization invalid: " + auth.getLocation());
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Authorization validation timed out for " + auth.getIdentifier());
    }

    private void pollAuthorizationValidWithRetry(final Authorization auth, final int maxSeconds, final long sleepMillis)
        throws InterruptedException, AcmeException {
        final long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                retryExecutor.callWithRetry("acme.auth.update", () -> {
                    auth.update();
                    return Boolean.TRUE;
                });
            } catch (final Exception e) {
                // If a non-transient error bubbles up, rethrow as IllegalStateException
                throw new IllegalStateException("Authorization update failed", e);
            }
            final var status = auth.getStatus();
            if (status == org.shredzone.acme4j.Status.VALID) {
                return;
            }
            if (status == org.shredzone.acme4j.Status.INVALID) {
                throw new IllegalStateException("Authorization invalid: " + auth.getLocation());
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Authorization validation timed out for " + auth.getIdentifier());
    }

    private void pollOrderValid(final Order order, final int maxSeconds, final long sleepMillis)
        throws InterruptedException, AcmeException {
        final long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            order.update();
            final var status = order.getStatus();
            if (status == org.shredzone.acme4j.Status.VALID) {
                return;
            }
            if (status == org.shredzone.acme4j.Status.INVALID) {
                throw new IllegalStateException("Order became INVALID");
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Order finalization timed out");
    }

    private void pollOrderValidWithRetry(final Order order, final int maxSeconds, final long sleepMillis)
        throws InterruptedException, AcmeException {
        final long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                retryExecutor.callWithRetry("acme.order.update", () -> {
                    order.update();
                    return Boolean.TRUE;
                });
            } catch (final Exception e) {
                throw new IllegalStateException("Order update failed", e);
            }
            final var status = order.getStatus();
            if (status == org.shredzone.acme4j.Status.VALID) {
                return;
            }
            if (status == org.shredzone.acme4j.Status.INVALID) {
                throw new IllegalStateException("Order became INVALID");
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Order finalization timed out");
    }

    private byte[] buildCsrDer(final String domain, final KeyPair keyPair)
        throws OperatorCreationException, java.io.IOException {
        final X500Name subject = new X500Name("CN=" + domain);
        final PKCS10CertificationRequestBuilder p10Builder =
            new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        final PKCS10CertificationRequest csr = p10Builder.build(signer);
        return csr.getEncoded();
    }

    private String toPem(final X509Certificate cert) {
        try {
            final var base64 = java.util.Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(cert.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----\n";
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to encode certificate to PEM", e);
        }
    }

    private void updateJobMessage(final CertificateJobEntity job, final String template, final Object... args) {
        final var message = String.format(template, args);
        job.setMessage(message);
        jobRepository.save(job);
        log.info(message);
    }
}
