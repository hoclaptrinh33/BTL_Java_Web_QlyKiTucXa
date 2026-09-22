package com.ktx.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ktx.domain.Permission;
import com.ktx.domain.Role;
import com.ktx.domain.User;
import com.ktx.domain.enums.AccountKind;

public class KtxUserDetails implements UserDetails {

    public static final Set<String> SYSTEM_PERMISSIONS = Set.of(
            "config.read",
            "config.write",
            "admin_account.manage"
    );

    public static final Set<String> OPERATION_PERMISSIONS = Set.of(
            "student.read", "student.write",
            "building.read", "building.write",
            "room.read", "room.write",
            "period.manage",
            "application.read",
            "allocation.manage",
            "contract.read", "contract.write",
            "invoice.read", "invoice.issue",
            "payment.record",
            "meter.read",
            "ticket.handle",
            "violation.write",
            "checkin.operate",
            "checkout.force",
            "report.read",
            "user.manage",
            "role.manage",
            "building.assign"
    );

    public static final Set<String> CAN_BO_PERMISSIONS = Set.of(
            "room.read",
            "ticket.handle",
            "violation.write",
            "checkin.operate",
            "meter.read"
    );

    private final User user;
    private final boolean isNotLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public KtxUserDetails(User user) {
        this(user, null, true);
    }

    public KtxUserDetails(User user, boolean isNotLocked) {
        this(user, null, isNotLocked);
    }

    public KtxUserDetails(User user, Collection<? extends GrantedAuthority> authorities, boolean isNotLocked) {
        this.user = user;
        this.isNotLocked = isNotLocked;
        this.authorities = authorities != null ? authorities : buildAuthorities(user);
    }

    public static Collection<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user == null) {
            return authorities;
        }

        AccountKind kind = user.getAccountKind();
        if (kind == AccountKind.STUDENT) {
            authorities.add(new SimpleGrantedAuthority("student.portal"));
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            return authorities;
        }

        // INTERNAL account
        Set<String> permissionCodes = new HashSet<>();
        boolean hasQuanLyRole = false;
        boolean hasCanBoRole = false;

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            for (Role role : user.getRoles()) {
                if (role == null) continue;
                if ("QUAN_LY".equalsIgnoreCase(role.getCode())) {
                    hasQuanLyRole = true;
                }
                if ("CAN_BO".equalsIgnoreCase(role.getCode())) {
                    hasCanBoRole = true;
                }
                if (role.getPermissions() != null) {
                    for (Permission perm : role.getPermissions()) {
                        if (perm != null && perm.getCode() != null) {
                            permissionCodes.add(perm.getCode());
                        }
                    }
                }
            }
        } else {
            // Fallback if roles collection is not loaded or user came from legacy code
            if (user.getRole() == com.ktx.domain.enums.Role.STAFF) {
                hasCanBoRole = true;
                permissionCodes.addAll(CAN_BO_PERMISSIONS);
            } else if (user.getRole() == com.ktx.domain.enums.Role.ADMIN) {
                hasQuanLyRole = true;
                permissionCodes.addAll(OPERATION_PERMISSIONS);
            }
        }

        // Add each permission code (without ROLE_ prefix)
        for (String code : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }

        // Check if user has ANY operation permission
        boolean hasOperationPermission = permissionCodes.stream()
                .anyMatch(OPERATION_PERMISSIONS::contains);

        // TƯƠNG THÍCH tạm:
        // INTERNAL có bất kỳ quyền OPERATION thì thêm ROLE_ADMIN nếu có vai QUAN_LY,
        // thêm ROLE_STAFF nếu chỉ có CAN_BO (không có QUAN_LY).
        // SYSTEM_ADMIN không được ROLE_ADMIN.
        if (hasOperationPermission) {
            if (hasQuanLyRole) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else if (hasCanBoRole) {
                authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
            }
        }

        return authorities;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isNotLocked;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getEnabled());
    }
}
