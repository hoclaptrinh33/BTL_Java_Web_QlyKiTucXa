package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        loginAttemptService.clearCache();
    }

    @Test
    void isBlocked_InitiallyFalse() {
        assertFalse(loginAttemptService.isBlocked("127.0.0.1", "user1"));
    }

    @Test
    void isBlocked_TrueAfterFiveAttempts() {
        String ip = "127.0.0.1";
        String username = "user1";

        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(ip, username);
            assertFalse(loginAttemptService.isBlocked(ip, username));
        }

        // 5th attempt
        loginAttemptService.loginFailed(ip, username);
        assertTrue(loginAttemptService.isBlocked(ip, username));
    }

    @Test
    void isBlocked_IsolatedByIpAndUsername() {
        loginAttemptService.loginFailed("127.0.0.1", "user1");
        loginAttemptService.loginFailed("127.0.0.1", "user1");
        loginAttemptService.loginFailed("127.0.0.1", "user1");
        loginAttemptService.loginFailed("127.0.0.1", "user1");
        loginAttemptService.loginFailed("127.0.0.1", "user1");

        // 127.0.0.1:user1 is locked
        assertTrue(loginAttemptService.isBlocked("127.0.0.1", "user1"));

        // Different user same IP is NOT locked
        assertFalse(loginAttemptService.isBlocked("127.0.0.1", "user2"));

        // Same user different IP is NOT locked
        assertFalse(loginAttemptService.isBlocked("192.168.1.1", "user1"));
    }

    @Test
    void loginSucceeded_ResetsAttempts() {
        String ip = "127.0.0.1";
        String username = "user1";

        loginAttemptService.loginFailed(ip, username);
        loginAttemptService.loginFailed(ip, username);
        loginAttemptService.loginFailed(ip, username);
        loginAttemptService.loginFailed(ip, username);

        assertFalse(loginAttemptService.isBlocked(ip, username));

        // Success resets attempts
        loginAttemptService.loginSucceeded(ip, username);

        // Failures start counting again
        loginAttemptService.loginFailed(ip, username);
        assertFalse(loginAttemptService.isBlocked(ip, username));
    }
}
