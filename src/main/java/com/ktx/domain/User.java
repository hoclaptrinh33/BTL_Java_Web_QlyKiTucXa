package com.ktx.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import com.ktx.domain.enums.AccountKind;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private com.ktx.domain.enums.Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_kind", nullable = false, length = 20)
    private AccountKind accountKind = AccountKind.INTERNAL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_buildings",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "building_id")
    )
    private Set<Building> assignedBuildings = new HashSet<>();

    @Column(nullable = false)
    private Boolean enabled;

    @Column
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public com.ktx.domain.enums.Role getRole() {
        return role;
    }

    public void setRole(com.ktx.domain.enums.Role role) {
        this.role = role;
        if (role == com.ktx.domain.enums.Role.STUDENT) {
            this.accountKind = AccountKind.STUDENT;
        } else if (this.accountKind == null || this.accountKind == AccountKind.STUDENT) {
            this.accountKind = AccountKind.INTERNAL;
        }
    }

    public AccountKind getAccountKind() {
        if (role == com.ktx.domain.enums.Role.STUDENT) {
            return AccountKind.STUDENT;
        }
        if (accountKind != null) {
            return accountKind;
        }
        return AccountKind.INTERNAL;
    }

    public void setAccountKind(AccountKind accountKind) {
        this.accountKind = accountKind;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public Set<Building> getAssignedBuildings() {
        return assignedBuildings;
    }

    public void setAssignedBuildings(Set<Building> assignedBuildings) {
        this.assignedBuildings = assignedBuildings != null ? assignedBuildings : new HashSet<>();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
