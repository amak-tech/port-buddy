/*
 * Copyright (c) 2026 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.entity;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {

    @EmbeddedId
    private UserAccountId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId")
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", nullable = false)
    private Set<Role> roles;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Constructs a new UserAccountEntity linking a user to an account with specific roles.
     *
     * @param user    the user.
     * @param account the account.
     * @param roles   the roles of the user within the account.
     */
    public UserAccountEntity(final UserEntity user, final AccountEntity account, final Set<Role> roles) {
        this.user = user;
        this.account = account;
        this.roles = roles;
        this.id = new UserAccountId(user.getId(), account.getId());
        this.lastUsedAt = OffsetDateTime.now();
    }

    /**
     * Gets the user's email by delegating to the associated UserEntity.
     *
     * @return the user's email.
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * Gets the user's first name by delegating to the associated UserEntity.
     *
     * @return the user's first name.
     */
    public String getFirstName() {
        return user.getFirstName();
    }

    /**
     * Gets the user's last name by delegating to the associated UserEntity.
     *
     * @return the user's last name.
     */
    public String getLastName() {
        return user.getLastName();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class UserAccountId implements Serializable {
        private UUID userId;
        private UUID accountId;
    }
}
