package com.ktx.web.staff;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffDashboardController {

    @GetMapping("/staff/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Bảng điều khiển tòa được gán");
        model.addAttribute("activeMenu", "dashboard");
        return "staff/dashboard";
    }
}
