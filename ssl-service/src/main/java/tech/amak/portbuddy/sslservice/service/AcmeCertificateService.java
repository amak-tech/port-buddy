/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.service;

import static java.time.ZoneOffset.UTC;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest;
import static org.bouncycastle.asn1.x509.GeneralName.dNSName;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.amak.portbuddy.sslservice.client.ServerClient;
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
    private final DnsResolverService dnsResolverService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final ServerClient serverClient;
    private final ObjectProvider<AcmeCertificateService> self;

    /**
     * Submits an asynchronous job to issue or renew a certificate for the given domain.
     *
     * @param domain      the domain to issue or renew certificate for
     * @param requestedBy username of requester
     * @param managed     whether the certificate should be managed (auto-renewed)
     * @return persisted job entity
     */
    @Transactional
    public CertificateJobEntity submitJob(final String domain, final String requestedBy, final boolean managed) {
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
        job.setManaged(managed);
        // If we have a managed certificate record, inherit contact email for notifications, 
        // otherwise use the one provided by the requester
        final var existingCert = certificateRepository.findByDomainIgnoreCase(normalizedDomain);
        if (existingCert.isPresent()) {
            job.setContactEmail(existingCert.get().getContactEmail());
        } else {
            job.setContactEmail(requestedBy);
        }
        final var savedJob = jobRepository.save(job);
        // Fire and forget async processing
        self.getIfAvailable().processJobAsync(savedJob.getId());
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

        try {
            MDC.put("jobId", String.valueOf(jobId));
            MDC.put("domain", job.getDomain());
            job.setStatus(CertificateJobStatus.RUNNING);
            job.setStartedAt(OffsetDateTime.now());
            jobRepository.save(job);

            // If the requested domain contains a wildcard, use manual DNS-01 flow with admin confirmation.
            if (job.getDomain().contains("*")) {
                performAcmeDns01Initiate(job);
            } else {
                performAcmeHttp01Issuance(job);
            }
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
            if (auth.getStatus() == Status.VALID) {
                continue;
            }
            final Http01Challenge httpChallenge = auth.getChallenges().stream()
                .filter(challenge -> challenge.getType().equals(Http01Challenge.TYPE))
                .map(challenge -> (Http01Challenge) challenge)
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
        final var fullChainPath = storageService.writeFullChainPem(domain, leafCertPem + chainPem);

        // 8) Update DB
        var certificate = certificateRepository.findByDomain(domain).orElse(null);
        if (certificate == null) {
            certificate = new CertificateEntity();
            certificate.setDomain(domain);
        }
        certificate.setManaged(job.isManaged());
        certificate.setContactEmail(job.getContactEmail());

        // Try to extract validity from leaf certificate
        final var x509 = downloaded.getCertificate();
        certificate.setStatus(CertificateStatus.ACTIVE);
        certificate.setIssuedAt(OffsetDateTime.ofInstant(x509.getNotBefore().toInstant(), UTC));
        certificate.setExpiresAt(OffsetDateTime.ofInstant(x509.getNotAfter().toInstant(), UTC));
        certificate.setPrivateKeyPath(keyPath.toAbsolutePath().toString());
        certificate.setCertificatePath(certPath.toAbsolutePath().toString());
        certificate.setChainPath(chainPath.toAbsolutePath().toString());
        certificate.setFullChainPath(fullChainPath.toAbsolutePath().toString());
        certificateRepository.save(certificate);

        // Notify server module about successful issuance
        try {
            serverClient.markSslActive(domain);
        } catch (final Exception e) {
            log.warn("Failed to notify server module about SSL activation for {}", domain, e);
        }

        // Single-entity model: no separate root-domain metadata to update

        job.setStatus(CertificateJobStatus.SUCCEEDED);
        job.setFinishedAt(OffsetDateTime.now());
        job.setMessage("Certificate issued/renewed successfully.");
        jobRepository.save(job);
    }

    /**
     * Initiates ACME DNS-01 flow for wildcard domains and pauses awaiting admin DNS confirmation.
     * Stores ACME order and authorization info as well as required TXT records in the job.
     *
     * @param job the job to process
     */
    private void performAcmeDns01Initiate(final CertificateJobEntity job) throws Exception {
        final var requestedDomain = job.getDomain();
        updateJobMessage(job, "Starting DNS-01 issuance for '%s'", requestedDomain);

        final Session session = acmeClientService.newSession();
        updateJobMessage(job, "ACME session created");

        final KeyPair accountKeyPair = acmeAccountService.loadAccountKeyPair();
        final Account account = retryExecutor
            .callWithRetry("acme.login", () -> acmeClientService.loginOrRegister(session, accountKeyPair));
        updateJobMessage(job, "Logged into ACME account");

        // Create order for apex + wildcard when wildcard requested, otherwise single domain
        final var domains = new ArrayList<String>();
        if (requestedDomain.startsWith("*.")) {
            final var apex = requestedDomain.substring(2);
            domains.add(apex);
        }
        domains.add(requestedDomain);

        final Order order = retryExecutor.callWithRetry("acme.order.create", () -> account
            .newOrder().domains(domains.toArray(String[]::new)).create());
        job.setOrderLocation(order.getLocation().toString());

        // Collect DNS-01 challenges and build instruction payload
        final var authorizations = order.getAuthorizations();
        final var authUrls = new ArrayList<String>();
        final var records = new ArrayList<Map<String, String>>();
        OffsetDateTime authExpiresAt = null;
        for (final Authorization auth : authorizations) {
            authUrls.add(auth.getLocation().toString());
            final var idDomain = auth.getIdentifier().getDomain();
            final var recordHost = idDomain.startsWith("*.") ? idDomain.substring(2) : idDomain;
            final var recordName = "_acme-challenge." + recordHost;
            final var dns01 = auth.getChallenges().stream()
                .filter(challenge -> challenge.getType().equals(Dns01Challenge.TYPE))
                .map(challenge -> (Dns01Challenge) challenge)
                .findFirst()
                .orElse(null);
            if (dns01 == null) {
                throw new IllegalStateException("DNS-01 challenge not available for domain: " + idDomain);
            }
            final var txtValue = dns01.getDigest();
            final var map = new HashMap<String, String>();
            map.put("name", recordName);
            map.put("value", txtValue);
            records.add(map);
            // Track the earliest authorization expiration across all identifiers
            final var expiresOpt = auth.getExpires();
            if (expiresOpt != null && expiresOpt.isPresent()) {
                final var expiresAt = OffsetDateTime.ofInstant(expiresOpt.get(), UTC);
                if (authExpiresAt == null || expiresAt.isBefore(authExpiresAt)) {
                    authExpiresAt = expiresAt;
                }
            }
        }
        job.setAuthorizationUrlsJson(objectMapper.writeValueAsString(authUrls));
        job.setChallengeRecordsJson(objectMapper.writeValueAsString(records));
        job.setChallengeExpiresAt(authExpiresAt);
        job.setStatus(CertificateJobStatus.WAITING_DNS_INSTRUCTIONS);
        jobRepository.save(job);

        // Send email to admin with instructions (best-effort)
        emailService.sendDnsInstructions(job, records, authExpiresAt);
        job.setStatus(CertificateJobStatus.AWAITING_ADMIN_CONFIRMATION);
        updateJobMessage(job, "Awaiting admin DNS TXT creation for %s", requestedDomain);
    }

    /**
     * Confirms DNS TXT records are in place and continues ACME DNS-01 flow to issuance.
     *
     * @param jobId certificate job id
     */
    @Transactional
    public void confirmDnsAndContinue(final UUID jobId) {
        final var job = jobRepository.findById(jobId).orElseThrow();
        MDC.put("jobId", String.valueOf(jobId));
        MDC.put("domain", job.getDomain());
        try {
            if (job.getStatus() != CertificateJobStatus.AWAITING_ADMIN_CONFIRMATION) {
                throw new IllegalStateException("Job is not awaiting admin confirmation");
            }
            job.setStatus(CertificateJobStatus.VERIFYING_DNS);
            jobRepository.save(job);

            final var records = objectMapper.readValue(job.getChallengeRecordsJson(),
                new TypeReference<List<Map<String, String>>>() {
                });

            // Verify TXT visibility for each record
            for (final var record : records) {
                final var name = record.get("name");
                final var value = record.get("value");
                updateJobMessage(job, "Checking TXT %s", name);
                retryExecutor.callWithRetry("dns.check." + name, () -> {
                    final var ok = dnsResolverService.isTxtRecordVisible(name, value);
                    if (!ok) {
                        throw new IllegalStateException("TXT record not visible yet: " + name);
                    }
                    return Boolean.TRUE;
                });
            }

            // Re-bind order and trigger challenges
            final Session session = acmeClientService.newSession();
            final KeyPair accountKeyPair = acmeAccountService.loadAccountKeyPair();
            final Account account = retryExecutor
                .callWithRetry("acme.login", () -> acmeClientService.loginOrRegister(session, accountKeyPair));
            final var orderLocation = job.getOrderLocation();
            final Order order = acmeClientService.bindOrder(session, account, accountKeyPair, orderLocation);

            final var authorizations = order.getAuthorizations();
            for (final Authorization auth : authorizations) {
                if (auth.getStatus() == Status.VALID) {
                    continue;
                }
                final var dns01 = auth.getChallenges().stream()
                    .filter(challenge -> challenge.getType().equals(Dns01Challenge.TYPE))
                    .map(challenge -> (Dns01Challenge) challenge)
                    .findFirst()
                    .orElse(null);
                if (dns01 == null) {
                    throw new IllegalStateException("DNS-01 challenge not available: " + auth.getIdentifier());
                }
                retryExecutor.callWithRetry("acme.challenge.trigger", () -> {
                    dns01.trigger();
                    return Boolean.TRUE;
                });
                pollAuthorizationValidWithRetry(auth, 180, 2_000);
            }

            // Generate key and CSR for apex + wildcard (or single domain)
            final var domain = job.getDomain();
            final KeyPair domainKeyPair = storageService.generateRsaKeyPair();
            final byte[] csr;
            if (domain.startsWith("*.")) {
                final var apex = domain.substring(2);
                csr = buildCsrDer(List.of(apex, domain), domainKeyPair);
            } else {
                csr = buildCsrDer(List.of(domain), domainKeyPair);
            }

            retryExecutor.callWithRetry("acme.order.finalize", () -> {
                order.execute(csr);
                return Boolean.TRUE;
            });
            updateJobMessage(job, "Order finalized, waiting for issuance");
            pollOrderValidWithRetry(order, 180, 2_000);

            final var downloaded = retryExecutor.callWithRetry("acme.cert.download", order::getCertificate);
            final var certChain = downloaded.getCertificateChain();
            final var leafCertPem = toPem(downloaded.getCertificate());
            final var chainPem = certChain == null
                ? ""
                : certChain.stream()
                .skip(1)
                .map(this::toPem)
                .reduce("", (a, b) -> a + b);

            final var keyPath = storageService.writePrivateKeyPem(domain, domainKeyPair);
            final var certPath = storageService.writeCertPem(domain, leafCertPem);
            final var chainPath = storageService.writeChainPem(domain, chainPem);
            final var fullChainPath = storageService.writeFullChainPem(domain, leafCertPem + chainPem);

            var certificate = certificateRepository.findByDomain(domain).orElse(null);
            if (certificate == null) {
                certificate = new CertificateEntity();
                certificate.setDomain(domain);
            }
            certificate.setManaged(job.isManaged());
            certificate.setContactEmail(job.getContactEmail());

            final var x509 = downloaded.getCertificate();
            certificate.setStatus(CertificateStatus.ACTIVE);
            certificate.setIssuedAt(OffsetDateTime.ofInstant(x509.getNotBefore().toInstant(), UTC));
            certificate.setExpiresAt(OffsetDateTime.ofInstant(x509.getNotAfter().toInstant(), UTC));
            certificate.setPrivateKeyPath(keyPath.toAbsolutePath().toString());
            certificate.setCertificatePath(certPath.toAbsolutePath().toString());
            certificate.setChainPath(chainPath.toAbsolutePath().toString());
            certificate.setFullChainPath(fullChainPath.toAbsolutePath().toString());
            certificateRepository.save(certificate);

            // Notify server module about successful issuance
            try {
                serverClient.markSslActive(domain);
            } catch (final Exception e) {
                log.warn("Failed to notify server module about SSL activation for {}", domain, e);
            }

            // Single-entity model: no separate root-domain metadata to update

            job.setStatus(CertificateJobStatus.SUCCEEDED);
            job.setFinishedAt(OffsetDateTime.now());
            job.setMessage("Certificate issued successfully.");
            jobRepository.save(job);
        } catch (final Exception e) {
            log.error("confirmDnsAndContinue failed", e);
            job.setStatus(CertificateJobStatus.FAILED);
            job.setFinishedAt(OffsetDateTime.now());
            job.setMessage(e.getMessage());
            jobRepository.save(job);
            throw new IllegalStateException("DNS confirmation/issuance failed", e);
        } finally {
            MDC.clear();
        }
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
            if (status == Status.VALID) {
                return;
            }
            if (status == Status.INVALID) {
                throw new IllegalStateException("Authorization invalid: " + auth.getLocation());
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Authorization validation timed out for " + auth.getIdentifier());
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
            if (status == Status.VALID) {
                return;
            }
            if (status == Status.INVALID) {
                throw new IllegalStateException("Order became INVALID");
            }
            Thread.sleep(sleepMillis);
        }
        throw new IllegalStateException("Order finalization timed out");
    }

    private byte[] buildCsrDer(final java.util.List<String> domains, final KeyPair keyPair)
        throws OperatorCreationException, java.io.IOException {
        final var primary = domains.get(0);
        final X500Name subject = new X500Name("CN=" + primary);
        final PKCS10CertificationRequestBuilder p10Builder =
            new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        // Add SAN extension with all domains
        final var genNames = domains.stream()
            .map(d -> new org.bouncycastle.asn1.x509.GeneralName(dNSName, d))
            .toArray(org.bouncycastle.asn1.x509.GeneralName[]::new);
        final var subjectAltName = new org.bouncycastle.asn1.x509.GeneralNames(genNames);
        final var extGen = new org.bouncycastle.asn1.x509.ExtensionsGenerator();
        extGen.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false, subjectAltName);
        final var extensions = extGen.generate();
        p10Builder.addAttribute(pkcs_9_at_extensionRequest, extensions);

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        final PKCS10CertificationRequest csr = p10Builder.build(signer);
        return csr.getEncoded();
    }

    private byte[] buildCsrDer(final String domain, final KeyPair keyPair)
        throws OperatorCreationException, java.io.IOException {
        return buildCsrDer(List.of(domain), keyPair);
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
