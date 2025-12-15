/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.sslservice.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "ssl_certificates")
@EntityListeners(AuditingEntityListener.class)
public class CertificateEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "domain", nullable = false, unique = true, length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CertificateStatus status = CertificateStatus.NEW;

    /**
     * Indicates that this certificate/domain is managed by the service (auto-renewal enabled).
     */
    @Column(name = "managed", nullable = false)
    private boolean managed = false;

    /**
     * Verification method for ACME challenges. For now: MANUAL_DNS01 or HTTP01.
     */
    @Column(name = "verification_method", length = 64)
    private String verificationMethod;

    /**
     * Optional contact email to notify about actions required (DNS setup) and results.
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "certificate_path", length = 1024)
    private String certificatePath;

    @Column(name = "private_key_path", length = 1024)
    private String privateKeyPath;

    @Column(name = "chain_path", length = 1024)
    private String chainPath;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
