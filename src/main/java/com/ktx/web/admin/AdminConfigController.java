package com.ktx.web.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.config.MailConfig;
import com.ktx.domain.SystemConfig;
import com.ktx.service.SystemConfigService;

@Controller
@RequestMapping("/admin/configs")
public class AdminConfigController {

    private final SystemConfigService systemConfigService;
    private final MailConfig mailConfig;
    private final com.ktx.service.AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminConfigController(SystemConfigService systemConfigService, MailConfig mailConfig,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) com.ktx.service.AuditLogService auditLogService) {
        this.systemConfigService = systemConfigService;
        this.mailConfig = mailConfig;
        this.auditLogService = auditLogService;
    }

    public AdminConfigController(SystemConfigService systemConfigService, MailConfig mailConfig) {
        this(systemConfigService, mailConfig, null);
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q, Model model) {
        Map<String, List<SystemConfig>> groupedConfigs = systemConfigService.getConfigsGrouped();

        model.addAttribute("groupedConfigs", groupedConfigs);
        model.addAttribute("mailEnabled", mailConfig.isMailEnabled());
        model.addAttribute("pageTitle", "Cài đặt hệ thống");
        model.addAttribute("pageSubtitle", "Quản trị tham số phân bổ, hợp đồng, đơn giá điện nước và điểm rèn luyện");
        model.addAttribute("activeMenu", "configs");
        model.addAttribute("q", q);

        return "admin/configs/index";
    }

    @PostMapping
    public String updateBatch(@RequestParam Map<String, String> allParams,
                              RedirectAttributes redirectAttributes) {
        Map<String, String> configsToUpdate = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!"_csrf".equals(entry.getKey())) {
                configsToUpdate.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            systemConfigService.updateConfigs(configsToUpdate);
            if (auditLogService != null) {
                auditLogService.logCurrent("CONFIG_UPDATE", "SYSTEM_CONFIG", "batch",
                        "Cập nhật cấu hình hệ thống hàng loạt (" + configsToUpdate.size() + " tham số)", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật cấu hình hệ thống thành công!");
        } catch (BusinessException | IllegalArgumentException e) {
            if (auditLogService != null) {
                auditLogService.logCurrent("CONFIG_UPDATE", "SYSTEM_CONFIG", "batch",
                        "Lỗi cập nhật cấu hình hàng loạt: " + e.getMessage(), "FAILURE");
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/configs";
    }

    @PostMapping("/single")
    public String updateSingle(@RequestParam("key") String key,
                               @RequestParam("value") String value,
                               RedirectAttributes redirectAttributes) {
        try {
            systemConfigService.set(key, value);
            if (auditLogService != null) {
                auditLogService.logCurrent("CONFIG_UPDATE", "SYSTEM_CONFIG", key,
                        "Cập nhật tham số cấu hình '" + key + "' = '" + value + "'", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật cấu hình '" + key + "' thành công!");
        } catch (BusinessException | IllegalArgumentException e) {
            if (auditLogService != null) {
                auditLogService.logCurrent("CONFIG_UPDATE", "SYSTEM_CONFIG", key,
                        "Lỗi cập nhật cấu hình '" + key + "': " + e.getMessage(), "FAILURE");
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/configs";
    }
}
