package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginSuccessHandlerTest {

    private final LoginSuccessHandler handler = new LoginSuccessHandler();

    @Test
    void adminGoesToAdminConfigs() {
        assertEquals("/admin/dashboard", handler.resolveTarget(auth("ROLE_ADMIN")));
        assertEquals("/admin/configs", handler.resolveTarget(auth("config.read")));
        assertEquals("/admin/configs", handler.resolveTarget(auth("ROLE_SYSTEM_ADMIN")));
    }

    @Test
    void staffGoesToManageDashboard() {
        assertEquals("/manage/dashboard", handler.resolveTarget(auth("ROLE_STAFF")));
        assertEquals("/manage/dashboard", handler.resolveTarget(auth("room.read")));
    }

    @Test
    void studentGoesToStudentDashboard() {
        assertEquals("/student/dashboard", handler.resolveTarget(auth("ROLE_STUDENT")));
    }

    @Test
    void systemAdminGoesToAdminConfigs() {
        assertEquals("/admin/configs", handler.resolveTarget(auth("config.read")));
    }

    private static UsernamePasswordAuthenticationToken auth(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "user", "n/a", List.of(new SimpleGrantedAuthority(role)));
    }
}
