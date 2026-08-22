package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ktx.service.DashboardService;

@Controller
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Bảng điều khiển hệ thống");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dash", dashboardService.load());
        return "admin/dashboard";
    }

    @GetMapping("/admin/stats")
    public String stats(Model model) {
        model.addAttribute("pageTitle", "Thống kê");
        model.addAttribute("pageSubtitle", "Occupancy, phòng và sinh viên nội trú theo tòa");
        model.addAttribute("activeMenu", "stats");
        model.addAttribute("dash", dashboardService.load());
        return "admin/stats";
    }
}
