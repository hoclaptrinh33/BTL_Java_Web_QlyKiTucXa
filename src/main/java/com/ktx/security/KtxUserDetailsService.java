package com.ktx.security;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;

@Service
public class KtxUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    public KtxUserDetailsService(UserRepository userRepository,
                                 StudentRepository studentRepository,
                                 LoginAttemptService loginAttemptService,
                                 HttpServletRequest request) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.loginAttemptService = loginAttemptService;
        this.request = request;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip, username)) {
            throw new LockedException("Tài khoản của bạn đã bị khóa tạm thời trong 10 phút do đăng nhập sai quá 5 lần.");
        }

        // Try to find by username or email first
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
        
        if (userOpt.isEmpty()) {
            // If not found, try to find student by studentCode (MSSV)
            userOpt = studentRepository.findByStudentCode(username)
                    .map(Student::getUser);
        }

        User user = userOpt.orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        return new KtxUserDetails(user);
    }

    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
