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

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String target;
        if (exception instanceof DisabledException) {
            target = "/login?disabled";
        } else if (exception instanceof LockedException) {
            target = "/login?locked";
        } else {
            target = "/login?error";
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
