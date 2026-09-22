package com.ktx.security;

import java.util.Collection;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Role;
import com.ktx.domain.User;
import com.ktx.repository.UserRepository;

@Service
public class KtxUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public KtxUserDetailsService(UserRepository userRepository,
                                 LoginAttemptService loginAttemptService,
                                 HttpServletRequest request) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role != null && role.getPermissions() != null) {
                    role.getPermissions().size();
                }
            }
        }
        if (user.getAssignedBuildings() != null) {
            user.getAssignedBuildings().size();
        }

        Collection<GrantedAuthority> authorities = KtxUserDetails.buildAuthorities(user);

        String ip = getClientIP();
        boolean isBlocked = loginAttemptService.isBlocked(ip, username);
        return new KtxUserDetails(user, authorities, !isBlocked);
    }

    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
