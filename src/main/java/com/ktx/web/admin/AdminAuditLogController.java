package com.ktx.web.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ktx.domain.AuditLog;
import com.ktx.dto.AuditLogSummaryDto;
import com.ktx.service.AuditLogService;

@Controller
@RequestMapping("/admin/logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String listLogs(@RequestParam(value = "q", required = false) String keyword,
                           @RequestParam(value = "action", required = false) String action,
                           @RequestParam(value = "targetType", required = false) String targetType,
                           @RequestParam(value = "status", required = false) String status,
                           @RequestParam(value = "from", required = false) String fromDate,
                           @RequestParam(value = "to", required = false) String toDate,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "20") int size,
                           Model model) {

        // Validate page bounds
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        Page<AuditLog> logPage = auditLogService.getLogs(keyword, action, targetType, status,
                fromDate, toDate, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        AuditLogSummaryDto summary = auditLogService.getSummary();

        model.addAttribute("pageTitle", "Nhật ký hoạt động");
        model.addAttribute("pageSubtitle", "Lịch sử thao tác, đăng nhập và thay đổi cấu hình hệ thống");
        model.addAttribute("activeMenu", "logs");

        model.addAttribute("logPage", logPage);
        model.addAttribute("summary", summary);
        model.addAttribute("actionsList", auditLogService.getDistinctActions());
        model.addAttribute("targetTypesList", auditLogService.getDistinctTargetTypes());

        // Echo search filters
        model.addAttribute("q", keyword);
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedTargetType", targetType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "admin/logs/index";
    }
}
