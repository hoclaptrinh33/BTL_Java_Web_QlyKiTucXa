package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginSuccessHandlerTest {

    private final LoginSuccessHandler handler = new LoginSuccessHandler();

    @Test
    void adminGoesToAdminDashboard() {
        assertEquals("/admin/dashboard", handler.resolveTarget(auth("ROLE_ADMIN")));
    }

    @Test
    void staffGoesToStaffDashboard() {
        assertEquals("/staff/dashboard", handler.resolveTarget(auth("ROLE_STAFF")));
    }

    @Test
    void studentGoesToStudentDashboard() {
        assertEquals("/student/dashboard", handler.resolveTarget(auth("ROLE_STUDENT")));
    }

    private static UsernamePasswordAuthenticationToken auth(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "user", "n/a", List.of(new SimpleGrantedAuthority(role)));
    }
}
