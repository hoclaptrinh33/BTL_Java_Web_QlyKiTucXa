package com.ktx.web.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentDashboardController {

    @GetMapping("/student/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Cổng thông tin sinh viên");
        model.addAttribute("activeMenu", "dashboard");
        return "student/dashboard";
    }
}
