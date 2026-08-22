package com.ktx.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ktx.domain.User;

public class KtxUserDetails implements UserDetails {

    private final User user;
    private final boolean isNotLocked;

    public KtxUserDetails(User user) {
        this.user = user;
        this.isNotLocked = true;
    }

    public KtxUserDetails(User user, boolean isNotLocked) {
        this.user = user;
        this.isNotLocked = isNotLocked;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
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
