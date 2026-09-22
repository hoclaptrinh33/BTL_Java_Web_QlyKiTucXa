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
        Set<String> authorities = new HashSet<>();
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (a != null && a.getAuthority() != null) {
                authorities.add(a.getAuthority());
            }
        }
        if (authorities.contains("ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        if (authorities.contains("ROLE_STAFF")) {
            return "/staff/dashboard";
        }
        if (authorities.contains("ROLE_STUDENT") || authorities.contains("student.portal")) {
            return "/student/dashboard";
        }
        if (authorities.contains("config.read") || authorities.contains("config.write")
                || authorities.contains("admin_account.manage")) {
            return "/admin/configs";
        }
        return "/login";
    }
}
