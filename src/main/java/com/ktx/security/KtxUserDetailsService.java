package com.ktx.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ktx.domain.User;
import com.ktx.repository.UserRepository;

@Service
public class KtxUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public KtxUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return new KtxUserDetails(user);
    }
}
