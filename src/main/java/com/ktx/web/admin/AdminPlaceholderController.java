package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPlaceholderController {

    @GetMapping("/admin/stats")
    public String stats(Model model) {
        return page(model, "stats", "Thống kê", "Biểu đồ lấp đầy và công nợ",
                "Dùng dashboard cho KPI chính.");
    }

    private static String page(Model model, String menu, String title, String subtitle, String hint) {
        model.addAttribute("activeMenu", menu);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("placeholderHint", hint);
        return "admin/placeholder";
    }
}
