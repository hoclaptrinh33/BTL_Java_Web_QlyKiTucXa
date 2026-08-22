package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

class LoginFailureHandlerTest {

    private LoginAttemptService loginAttemptService;
    private LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        loginAttemptService = mock(LoginAttemptService.class);
        handler = new LoginFailureHandler(loginAttemptService);
    }

    @Test
    void badCredentialsRedirectsToError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "testuser");
        when(loginAttemptService.isBlocked(anyString(), anyString())).thenReturn(false);
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));
        assertEquals("/login?error", response.getRedirectedUrl());
    }

    @Test
    void disabledRedirectsToDisabled() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new DisabledException("off"));
        assertEquals("/login?disabled", response.getRedirectedUrl());
    }

    @Test
    void lockedRedirectsToLocked() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new org.springframework.security.authentication.LockedException("locked"));
        assertEquals("/login?locked", response.getRedirectedUrl());
    }

    @Test
    void badCredentialsWhenBlockedRedirectsToLocked() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "blockeduser");
        when(loginAttemptService.isBlocked(anyString(), anyString())).thenReturn(true);
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));
        assertEquals("/login?locked", response.getRedirectedUrl());
    }
}
