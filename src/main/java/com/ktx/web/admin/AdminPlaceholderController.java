package com.ktx.web.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ktx.dto.DashboardSnapshot;
import com.ktx.service.DashboardService;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('report.read')")
public class AdminPlaceholderController {

    private final DashboardService dashboardService;

    public AdminPlaceholderController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/manage/stats", "/admin/stats"})
    public String stats(Model model) {
        DashboardSnapshot dash = dashboardService.load();
        model.addAttribute("activeMenu", "stats");
        model.addAttribute("pageTitle", "Thống kê & Phân tích");
        model.addAttribute("pageSubtitle", "Báo cáo phân tích tỷ lệ lấp đầy, cơ cấu lưu trú và hiện trạng vận hành KTX");
        model.addAttribute("dash", dash);
        return "admin/stats";
    }
}
