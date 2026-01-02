/*
 * Copyright (c) 2025 AMAK Inc. All rights reserved.
 */

package tech.amak.portbuddy.server.db.entity;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserAccountEntity> accounts;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "password")
    private String password;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Helper method to get the account of the user.
     * Since a user can have multiple accounts, this returns the first one in the set.
     * This is primarily used for backward compatibility in controllers.
     *
     * @return the first associated AccountEntity.
     * @throws IllegalStateException if the user has no accounts.
     */
    public AccountEntity getAccount() {
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalStateException("User has no accounts");
        }
        return accounts.iterator().next().getAccount();
    }

    /**
     * Helper method to get the roles of the user.
     * Since a user can have multiple accounts, this returns roles from the first account in the set.
     * This is primarily used for backward compatibility in controllers.
     *
     * @return a set of roles.
     * @throws IllegalStateException if the user has no accounts.
     */
    public Set<Role> getRoles() {
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalStateException("User has no accounts");
        }
        return accounts.iterator().next().getRoles();
    }
}
