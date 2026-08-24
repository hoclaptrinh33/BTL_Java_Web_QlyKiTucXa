package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPlaceholderController {

    @GetMapping("/admin/periods")
    public String periods(Model model) {
        return page(model, "periods", "Đợt đăng ký", "Mở, đóng đợt nộp đơn ở KTX",
                "Sinh viên chỉ nộp đơn trong đợt OPEN.");
    }

    @GetMapping("/admin/allocations")
    public String allocations(Model model) {
        return page(model, "allocations", "Phân bổ chỗ ở", "Xem trước và chốt giường theo điểm ưu tiên",
                "Preview không khóa giường. Chốt phân bổ mới tạo hợp đồng nháp.");
    }

    @GetMapping("/admin/payments")
    public String payments(Model model) {
        return page(model, "payments", "Thanh toán", "Ghi nhận tiền mặt / chuyển khoản tại quầy",
                "Không kết nối cổng online.");
    }

    @GetMapping("/admin/invoices")
    public String invoices(Model model) {
        return page(model, "invoices", "Hóa đơn", "Mọi khoản phí của sinh viên nội trú",
                "Tiền phòng theo kỳ, đặt cọc, điện nước và phụ phí vệ sinh / internet / gửi xe.");
    }

    @GetMapping("/admin/tickets")
    public String tickets(Model model) {
        return page(model, "tickets", "Yêu cầu sửa chữa", "Ticket sự cố phòng và tài sản",
                "Sinh viên tạo ticket cho phòng mình.");
    }

    @GetMapping("/admin/violations")
    public String violations(Model model) {
        return page(model, "violations", "Báo cáo vi phạm", "Biên bản và điểm rèn luyện KTX",
                "0 điểm thì sinh viên bị chặn nộp đơn ở mới.");
    }

    @GetMapping("/admin/check-in-out")
    public String checkInOut(Model model) {
        return page(model, "checkin", "Lịch sử ra vào", "Check-in / check-out theo hợp đồng",
                "Check-in đổi HĐ nháp thành đang ở. Check-out mới nhả giường.");
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        return page(model, "reports", "Báo cáo", "Xuất Excel / PDF danh sách nội trú và công nợ",
                "Sinh viên đang ở, hóa đơn quá hạn, lấp đầy theo tòa.");
    }



    @GetMapping("/admin/configs")
    public String configs(Model model) {
        return page(model, "configs", "Cài đặt hệ thống", "Trọng số phân bổ, giá điện nước, điểm rèn luyện",
                "Đổi cấu hình trên system_configs.");
    }

    private static String page(Model model, String menu, String title, String subtitle, String hint) {
        model.addAttribute("activeMenu", menu);
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("placeholderHint", hint);
        return "admin/placeholder";
    }
}
