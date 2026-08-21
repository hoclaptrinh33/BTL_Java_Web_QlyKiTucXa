package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Bảng điều khiển hệ thống");
        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }
}
