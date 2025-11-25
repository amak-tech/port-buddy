/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tunnels")
public class TunnelEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tunnel_id", nullable = false, unique = true)
    private String tunnelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TunnelType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TunnelStatus status;

    // Ownership
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "api_key_id")
    private UUID apiKeyId;

    // Local resource details
    @Column(name = "local_scheme")
    private String localScheme;

    @Column(name = "local_host")
    private String localHost;

    @Column(name = "local_port")
    private Integer localPort;

    // Public exposure details
    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "public_host")
    private String publicHost;

    @Column(name = "public_port")
    private Integer publicPort;

    @Column(name = "subdomain")
    private String subdomain;

    @Column(name = "last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
