package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPlaceholderController {

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        return page(model, "reports", "Báo cáo", "Xuất Excel / PDF danh sách nội trú và công nợ",
                "Sinh viên đang ở, hóa đơn quá hạn, lấp đầy theo tòa.");
    }

    private static String page(Model model, String menu, String title, String subtitle, String hint) {
        model.addAttribute("activeMenu", menu);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("placeholderHint", hint);
        return "admin/placeholder";
    }
}
