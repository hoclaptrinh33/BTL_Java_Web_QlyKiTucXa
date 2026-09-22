package com.ktx.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler, LoginFailureHandler loginFailureHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/login", "/login/google", "/oauth2/**", "/login/oauth2/**",
                        "/register", "/register/google", "/error", "/error/403", "/error/404").permitAll()
                // Specific matcher for system configs
                .requestMatchers("/admin/configs", "/admin/configs/**")
                    .hasAnyAuthority("config.read", "config.write", "admin_account.manage")
                // Specific matchers for profile and password
                .requestMatchers("/admin/profile", "/admin/profile/**", "/admin/password", "/admin/password/**")
                    .hasAnyAuthority("config.read", "config.write", "admin_account.manage", "ROLE_ADMIN")
                // General admin matchers: QUAN_LY (ROLE_ADMIN or operation permissions), while staff and SYSTEM_ADMIN are blocked
                .requestMatchers("/admin/**")
                    .access((authentication, context) -> {
                        Authentication a = authentication.get();
                        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
                            return new AuthorizationDecision(false);
                        }
                        boolean hasAdminRole = a.getAuthorities().stream()
                                .anyMatch(authItem -> "ROLE_ADMIN".equals(authItem.getAuthority()));
                        if (hasAdminRole) {
                            return new AuthorizationDecision(true);
                        }
                        boolean isStaff = a.getAuthorities().stream()
                                .anyMatch(authItem -> "ROLE_STAFF".equals(authItem.getAuthority()));
                        if (isStaff) {
                            return new AuthorizationDecision(false);
                        }
                        boolean hasOp = a.getAuthorities().stream()
                                .anyMatch(authItem -> KtxUserDetails.OPERATION_PERMISSIONS.contains(authItem.getAuthority()));
                        return new AuthorizationDecision(hasOp);
                    })
                // Staff endpoints
                .requestMatchers("/staff/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF",
                            "room.read", "ticket.handle", "violation.write", "checkin.operate", "meter.read")
                // Student endpoints
                .requestMatchers("/student/**")
                    .hasAnyAuthority("student.portal", "ROLE_STUDENT")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
            .exceptionHandling(e -> e.accessDeniedPage("/error/403"))
            .sessionManagement(s -> s
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired"));
        return http.build();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
