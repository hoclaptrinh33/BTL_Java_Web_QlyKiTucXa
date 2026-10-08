package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.ktx.domain.AuditLog;
import com.ktx.domain.User;
import com.ktx.dto.AuditLogSummaryDto;
import com.ktx.repository.AuditLogRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.impl.AuditLogServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, userRepository);
    }

    @Test
    void log_savesAuditLogWithUserAndCorrectFields() {
        User user = new User();
        user.setId(10L);
        user.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditLogService.log("admin", "ADMIN", "CONFIG_UPDATE",
                "SYSTEM_CONFIG", "alloc.preference.mode", "Cập nhật tham số", "127.0.0.1", "SUCCESS");

        assertNotNull(saved);
        assertEquals(10L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("ADMIN", saved.getUserRole());
        assertEquals("CONFIG_UPDATE", saved.getAction());
        assertEquals("SYSTEM_CONFIG", saved.getTargetType());
        assertEquals("alloc.preference.mode", saved.getTargetId());
        assertEquals("Cập nhật tham số", saved.getDescription());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("SUCCESS", saved.getStatus());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("CONFIG_UPDATE", captor.getValue().getAction());
    }

    @Test
    void getLogs_callsRepositoryFindWithFilters() {
        AuditLog log = new AuditLog(1L, "admin", "ADMIN", "LOGIN_SUCCESS", "AUTH", "admin", "Đăng nhập", "127.0.0.1", "SUCCESS");
        Page<AuditLog> mockPage = new PageImpl<>(List.of(log));

        when(auditLogRepository.findWithFilters(eq("admin"), eq("LOGIN_SUCCESS"), eq("AUTH"),
                eq("SUCCESS"), any(), any(), any(PageRequest.class))).thenReturn(mockPage);

        Page<AuditLog> result = auditLogService.getLogs("admin", "LOGIN_SUCCESS", "AUTH",
                "SUCCESS", "2026-09-01", "2026-09-22", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("LOGIN_SUCCESS", result.getContent().get(0).getAction());
    }

    @Test
    void getSummary_returnsCorrectSummaryStats() {
        when(auditLogRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(15L);
        when(auditLogRepository.countByActionStartingWithAndCreatedAtAfter(eq("LOGIN_SUCCESS"), any(LocalDateTime.class))).thenReturn(8L);
        when(auditLogRepository.countByStatusAndCreatedAtAfter(eq("FAILURE"), any(LocalDateTime.class))).thenReturn(2L);
        when(auditLogRepository.countByActionStartingWithAndCreatedAtAfter(eq("CONFIG_"), any(LocalDateTime.class))).thenReturn(5L);

        AuditLogSummaryDto summary = auditLogService.getSummary();

        assertNotNull(summary);
        assertEquals(15L, summary.getTodayTotal());
        assertEquals(8L, summary.getTodayLogins());
        assertEquals(2L, summary.getTodayFailures());
        assertEquals(5L, summary.getTodayConfigChanges());
    }
}
