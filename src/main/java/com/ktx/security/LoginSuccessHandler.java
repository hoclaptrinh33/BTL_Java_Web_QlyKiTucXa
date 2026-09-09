package com.ktx.security;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        redirectStrategy.sendRedirect(request, response, resolveTarget(authentication));
    }

    public String resolveTarget(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "/login";
        }
        Set<String> roles = new HashSet<>();
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (a != null && a.getAuthority() != null) {
                roles.add(a.getAuthority());
            }
        }
        if (roles.contains("ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        if (roles.contains("ROLE_STAFF")) {
            return "/staff/dashboard";
        }
        if (roles.contains("ROLE_STUDENT")) {
            return "/student/dashboard";
        }
        return "/login";
    }
}
