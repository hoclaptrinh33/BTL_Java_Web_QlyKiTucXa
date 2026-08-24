package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class KtxUserDetailsServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    private KtxUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new KtxUserDetailsService(userRepository, loginAttemptService, request);
    }

    @Test
    void loadUserByUsername_findsByUsername() {
        User user = user("sv001", "sv001@example.com", Role.STUDENT, true);
        when(userRepository.findByUsernameOrEmail("sv001", "sv001")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("sv001");

        assertEquals("sv001", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
        assertTrue(details.isEnabled());
    }

    @Test
    void loadUserByUsername_findsByEmail() {
        User user = user("admin", "admin@ktx.local", Role.ADMIN, true);
        when(userRepository.findByUsernameOrEmail("admin@ktx.local", "admin@ktx.local"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@ktx.local");

        assertEquals("admin", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_unknown_throwsUsernameNotFoundException() {
        when(userRepository.findByUsernameOrEmail("missing", "missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }

    @Test
    void loadUserByUsername_disabled_userDetailsNotEnabled() {
        User user = user("locked", "locked@ktx.local", Role.STAFF, false);
        when(userRepository.findByUsernameOrEmail("locked", "locked")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("locked");

        assertFalse(details.isEnabled());
    }

    @Test
    void authenticate_disabled_throwsDisabledException() {
        User user = user("locked", "locked@ktx.local", Role.STUDENT, false);
        when(userRepository.findByUsernameOrEmail("locked", "locked")).thenReturn(Optional.of(user));

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(ENCODER);

        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated("locked", "password");

        assertThrows(DisabledException.class, () -> provider.authenticate(token));
    }

    @Test
    void loadUserByUsername_blocked_returnsLockedUserDetails() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(loginAttemptService.isBlocked("127.0.0.1", "blockedUser")).thenReturn(true);
        User user = user("blockedUser", "blocked@ktx.local", Role.STUDENT, true);
        when(userRepository.findByUsernameOrEmail("blockedUser", "blockedUser")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("blockedUser");
        assertFalse(details.isAccountNonLocked());
    }

    @Test
    void authenticate_blocked_throwsLockedException() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(loginAttemptService.isBlocked("127.0.0.1", "blockedUser")).thenReturn(true);
        User user = user("blockedUser", "blocked@ktx.local", Role.STUDENT, true);
        when(userRepository.findByUsernameOrEmail("blockedUser", "blockedUser")).thenReturn(Optional.of(user));

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(ENCODER);

        UsernamePasswordAuthenticationToken token =
                UsernamePasswordAuthenticationToken.unauthenticated("blockedUser", "password");

        assertThrows(org.springframework.security.authentication.LockedException.class,
                () -> provider.authenticate(token));
    }

    private static User user(String username, String email, Role role, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPasswordHash(ENCODER.encode("password"));
        return user;
    }
}
