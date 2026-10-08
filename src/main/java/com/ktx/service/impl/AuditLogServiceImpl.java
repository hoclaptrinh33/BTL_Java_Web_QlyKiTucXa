package com.ktx.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ktx.domain.AuditLog;
import com.ktx.domain.User;
import com.ktx.dto.AuditLogSummaryDto;
import com.ktx.repository.AuditLogRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.AuditLogService;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String username, String userRole, String action, String targetType,
                        String targetId, String description, String ipAddress, String status) {
        try {
            Long userId = null;
            if (username != null && !username.isBlank()) {
                userId = userRepository.findByUsername(username).map(User::getId).orElse(null);
            }

            AuditLog auditLog = new AuditLog(userId,
                    username != null ? username : "anonymous",
                    userRole,
                    action != null ? action : "UNKNOWN",
                    targetType,
                    targetId,
                    description != null ? description : "",
                    ipAddress,
                    status != null ? status : "SUCCESS");

            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Lỗi khi ghi Audit Log: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logCurrent(String action, String targetType, String targetId,
                               String description, String status) {
        String username = "system";
        String userRole = "SYSTEM";
        String ipAddress = "127.0.0.1";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            username = auth.getName();
            userRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .map(r -> r.replace("ROLE_", ""))
                    .orElse("USER");
        }

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                ipAddress = getClientIP(req);
            }
        } catch (Exception ignored) {
        }

        return log(username, userRole, action, targetType, targetId, description, ipAddress, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(String keyword, String action, String targetType,
                                  String status, String fromDate, String toDate, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cleanAction = (action != null && !action.isBlank()) ? action.trim() : null;
        String cleanTargetType = (targetType != null && !targetType.isBlank()) ? targetType.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim() : null;

        LocalDateTime from = null;
        if (fromDate != null && !fromDate.isBlank()) {
            try {
                from = LocalDate.parse(fromDate.trim()).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        LocalDateTime to = null;
        if (toDate != null && !toDate.isBlank()) {
            try {
                to = LocalDate.parse(toDate.trim()).atTime(LocalTime.MAX);
            } catch (DateTimeParseException ignored) {
            }
        }

        return auditLogRepository.findWithFilters(cleanKeyword, cleanAction, cleanTargetType,
                cleanStatus, from, to, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogSummaryDto getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayTotal = auditLogRepository.countByCreatedAtAfter(startOfToday);
        long todayLogins = auditLogRepository.countByActionStartingWithAndCreatedAtAfter("LOGIN_SUCCESS", startOfToday);
        long todayFailures = auditLogRepository.countByStatusAndCreatedAtAfter("FAILURE", startOfToday);
        long todayConfigChanges = auditLogRepository.countByActionStartingWithAndCreatedAtAfter("CONFIG_", startOfToday);

        return new AuditLogSummaryDto(todayTotal, todayLogins, todayFailures, todayConfigChanges);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        return List.of("LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT", "CONFIG_UPDATE",
                "ADMIN_CREATE", "ADMIN_TOGGLE_STATUS", "ADMIN_DELETE", "ADMIN_RESET_PASSWORD",
                "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_ASSIGN",
                "USER_CREATE", "USER_UPDATE", "USER_TOGGLE_STATUS", "USER_RESET_PASSWORD", "PASSWORD_CHANGE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctTargetTypes() {
        return List.of("AUTH", "SYSTEM_CONFIG", "ADMIN_ACCOUNT", "ROLE", "USER", "ROOM", "ALLOCATION");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
