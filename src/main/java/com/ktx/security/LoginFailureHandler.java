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
    private final com.ktx.service.AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginFailureHandler(LoginAttemptService loginAttemptService,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.ktx.service.AuditLogService auditLogService) {
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
    }

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this(loginAttemptService, null);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = getClientIP(request);

        String target;
        String reason;
        if (exception instanceof DisabledException) {
            target = "/login?disabled";
            reason = "Tài khoản bị vô hiệu hóa";
        } else if (exception instanceof LockedException || loginAttemptService.isBlocked(ip, username)) {
            target = "/login?locked";
            reason = "Tài khoản tạm thời bị khóa do nhập sai nhiều lần";
        } else {
            target = "/login?error";
            reason = "Sai tên đăng nhập hoặc mật khẩu";
        }

        if (auditLogService != null) {
            auditLogService.log(username != null ? username : "anonymous", "GUEST",
                    "LOGIN_FAILED", "AUTH", username,
                    "Đăng nhập thất bại: " + reason, ip, "FAILURE");
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
