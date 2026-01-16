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

package tech.amak.portbuddy.server.db.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.amak.portbuddy.common.TunnelType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tunnels")
public class TunnelEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TunnelType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TunnelStatus status;

    // Ownership
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private DomainEntity domain;

    // Optional temporary passcode hash set for this tunnel via CLI
    @Column(name = "temp_passcode_hash")
    private String tempPasscodeHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port_reservation_id")
    private PortReservationEntity portReservation;

    @Column(name = "last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
