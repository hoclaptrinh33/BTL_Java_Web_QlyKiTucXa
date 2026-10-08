package com.ktx.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ktx.domain.AuditLog;
import com.ktx.dto.AuditLogSummaryDto;

public interface AuditLogService {

    AuditLog log(String username, String userRole, String action, String targetType,
                 String targetId, String description, String ipAddress, String status);

    AuditLog logCurrent(String action, String targetType, String targetId,
                        String description, String status);

    Page<AuditLog> getLogs(String keyword, String action, String targetType,
                           String status, String fromDate, String toDate, Pageable pageable);

    AuditLogSummaryDto getSummary();

    List<String> getDistinctActions();

    List<String> getDistinctTargetTypes();
}
