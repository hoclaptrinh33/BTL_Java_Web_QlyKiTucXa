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

    public static final Set<String> SYSTEM_PERMISSIONS = Set.of(
            "config.read", "config.write", "admin_account.manage"
    );

    public static final Set<String> OPERATION_PERMISSIONS = Set.of(
            "student.read", "student.write", "building.read", "building.write",
            "room.read", "room.write", "period.manage", "application.read",
            "allocation.manage", "contract.read", "contract.write", "invoice.read",
            "invoice.issue", "payment.record", "meter.read", "ticket.handle",
            "violation.write", "checkin.operate", "checkout.force", "report.read",
            "user.manage", "role.manage", "building.assign"
    );

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
        if (authorities.contains("ROLE_STUDENT") || authorities.contains("student.portal")) {
            return "/student/dashboard";
        }
        boolean hasOperation = authorities.stream().anyMatch(OPERATION_PERMISSIONS::contains)
                || authorities.contains("ROLE_STAFF");
        if (hasOperation) {
            return "/manage/dashboard";
        }
        if (authorities.contains("ROLE_ADMIN")) {
            return "/admin/dashboard";
        }
        boolean hasSystem = authorities.stream().anyMatch(SYSTEM_PERMISSIONS::contains)
                || authorities.contains("ROLE_SYSTEM_ADMIN");
        if (hasSystem) {
            return "/admin/configs";
        }
        return "/login";
    }
}
