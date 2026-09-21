package com.ktx.web.staff;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaffPlaceholderController {

    @GetMapping("/staff/checkin")
    public String checkin(Model model) {
        return page(model, "checkin", "Check-in sinh viên", "Bàn giao giường và tài sản theo hợp đồng",
                "Chọn hợp đồng nháp để check-in bàn giao phòng.");
    }

    @GetMapping("/staff/readings")
    public String readings(Model model) {
        return page(model, "readings", "Ghi chỉ số điện nước", "Nhập chỉ số điện và nước theo tháng",
                "Hỗ trợ ghi chỉ số và thay công tơ độc lập theo phòng.");
    }

    @GetMapping("/staff/invoices")
    public String invoices(Model model) {
        return page(model, "invoices", "Hóa đơn phòng", "Hóa đơn tiền phòng và điện nước của tòa",
                "Quản lý hóa đơn thu tiền của sinh viên trong tòa.");
    }

    @GetMapping("/staff/tickets")
    public String tickets(Model model) {
        return page(model, "tickets", "Yêu cầu sửa chữa", "Xử lý yêu cầu sửa chữa cơ sở vật chất tòa",
                "Tiếp nhận và cập nhật tiến độ xử lý ticket của sinh viên.");
    }

    @GetMapping("/staff/violations")
    public String violations(Model model) {
        return page(model, "violations", "Báo cáo vi phạm", "Lập biên bản vi phạm nội quy KTX",
                "Ghi nhận vi phạm và trừ điểm rèn luyện sinh viên.");
    }

    private static String page(Model model, String menu, String title, String subtitle, String hint) {
        model.addAttribute("activeMenu", menu);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("placeholderHint", hint);
        return "admin/placeholder";
    }
}
