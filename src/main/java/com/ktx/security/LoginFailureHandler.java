package com.ktx.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = getClientIP(request);

        String target;
        if (exception instanceof DisabledException) {
            target = "/login?disabled";
        } else if (exception instanceof LockedException || loginAttemptService.isBlocked(ip, username)) {
            target = "/login?locked";
        } else {
            target = "/login?error";
        }
        response.sendRedirect(request.getContextPath() + target);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
