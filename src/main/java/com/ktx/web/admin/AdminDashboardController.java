package com.ktx.web.admin;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ktx.config.MailConfig;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.DebtByMonthDto;
import com.ktx.repository.UserRepository;
import com.ktx.service.DashboardService;
import com.ktx.service.SystemConfigService;

@Controller
public class AdminDashboardController {

    private final SystemConfigService systemConfigService;
    private final MailConfig mailConfig;
    private final UserRepository userRepository;
    private final DashboardService dashboardService;

    public AdminDashboardController(@org.springframework.beans.factory.annotation.Autowired(required = false) SystemConfigService systemConfigService,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) MailConfig mailConfig,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) UserRepository userRepository,
                                    @org.springframework.beans.factory.annotation.Autowired(required = false) DashboardService dashboardService) {
        this.systemConfigService = systemConfigService;
        this.mailConfig = mailConfig;
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model) {
        Map<String, List<SystemConfig>> groupedConfigs = systemConfigService != null
                ? systemConfigService.getConfigsGrouped()
                : Map.of();
        int totalConfigs = groupedConfigs.values().stream().mapToInt(List::size).sum();

        List<User> admins = userRepository != null
                ? userRepository.findByRoleIn(List.of(Role.ADMIN))
                : List.of();
        long activeAdmins = admins.stream().filter(u -> Boolean.TRUE.equals(u.getEnabled())).count();

        model.addAttribute("pageTitle", "Tổng quan hệ thống");
        model.addAttribute("pageSubtitle", "Tóm tắt cấu hình tham số hệ thống KTX");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("groupedConfigs", groupedConfigs);
        model.addAttribute("totalConfigs", totalConfigs);
        model.addAttribute("groupCount", groupedConfigs.size());
        model.addAttribute("mailEnabled", mailConfig != null && mailConfig.isMailEnabled());
        model.addAttribute("adminCount", admins.size());
        model.addAttribute("activeAdminCount", activeAdmins);
        return "admin/dashboard";
    }

    @GetMapping("/admin/dashboard/api/occupancy")
    @ResponseBody
    public Map<String, Object> apiOccupancy() {
        return dashboardService.getOccupancyChartData();
    }

    @GetMapping("/admin/dashboard/api/debt-by-month")
    @ResponseBody
    public DebtByMonthDto apiDebtByMonth() {
        return dashboardService.calculateDebtByMonth();
    }
}
