package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPlaceholderController {



    @org.springframework.beans.factory.annotation.Autowired
    private com.ktx.repository.StudentRepository studentRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ktx.repository.BedRepository bedRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ktx.repository.ContractRepository contractRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ktx.repository.RoomApplicationRepository roomApplicationRepository;

    @GetMapping("/admin/allocations")
    public String allocations(Model model) {
        return page(model, "allocations", "Phân bổ chỗ ở", "Xem trước và chốt giường theo điểm ưu tiên",
                "Preview không khóa giường. Chốt phân bổ mới tạo hợp đồng nháp.");
    }

    @GetMapping("/dev-test/assign")
    @org.springframework.transaction.annotation.Transactional
    public String devTestAssign(org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // Lấy username của sinh viên đang đăng nhập hiện tại
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (auth != null) ? auth.getName() : "quanquan1";
        
        // Nếu admin click hoặc chưa đăng nhập, mặc định gán cho quanquan1
        if ("anonymousUser".equals(currentUsername) || "admin".equalsIgnoreCase(currentUsername) || "staffA".equalsIgnoreCase(currentUsername)) {
            currentUsername = "quanquan1";
        }

        com.ktx.domain.Student student = studentRepository.findByUserUsername(currentUsername).orElse(null);
        if (student == null) {
            student = studentRepository.findByUserUsername("D22CQCN001").orElse(null);
        }
        if (student == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sinh viên: " + currentUsername);
            return "redirect:/student/dashboard";
        }

        // Tự động dọn dẹp hợp đồng cũ nếu có để cho phép gán lại đúng tòa
        java.util.List<com.ktx.domain.Contract> oldContracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), com.ktx.common.util.OccupyingStatuses.OCCUPYING);
        for (com.ktx.domain.Contract oc : oldContracts) {
            com.ktx.domain.Bed oldBed = oc.getBed();
            if (oldBed != null) {
                oldBed.setStatus(com.ktx.domain.enums.BedStatus.VACANT);
                bedRepository.save(oldBed);
            }
            contractRepository.delete(oc);
        }

        java.util.List<com.ktx.domain.Bed> beds = bedRepository.findAll();
        com.ktx.domain.enums.BuildingGenderPolicy expectedPolicy = (student.getGender() == com.ktx.domain.enums.Gender.MALE) 
                ? com.ktx.domain.enums.BuildingGenderPolicy.MALE 
                : com.ktx.domain.enums.BuildingGenderPolicy.FEMALE;

        com.ktx.domain.Bed bed = beds.stream()
                .filter(b -> b.getStatus() == com.ktx.domain.enums.BedStatus.VACANT)
                .filter(b -> b.getRoom() != null && b.getRoom().getBuilding() != null && b.getRoom().getBuilding().getGenderPolicy() == expectedPolicy)
                .findFirst().orElse(null);

        if (bed == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không có giường trống! Hãy vào mục Phòng ở -> Thêm phòng hàng loạt để tạo phòng/giường trước.");
            return "redirect:/admin/rooms";
        }

        com.ktx.domain.RoomApplication app = roomApplicationRepository.findByStudentIdOrderByPeriodOpenAtDesc(student.getId())
                .stream().findFirst().orElse(null);

        com.ktx.domain.Contract c = new com.ktx.domain.Contract();
        c.setContractNo("HD-TEST-" + System.currentTimeMillis());
        c.setStudent(student);
        c.setBed(bed);
        c.setApplication(app);
        c.setStatus(com.ktx.domain.enums.ContractStatus.ACTIVE);
        c.setStartDate(java.time.LocalDate.now());
        c.setEndDate(java.time.LocalDate.now().plusMonths(5));
        c.setRoomFee(new java.math.BigDecimal("1200000"));
        c.setDepositAmount(new java.math.BigDecimal("600000"));
        c.setDepositStatus(com.ktx.domain.enums.DepositStatus.HELD);
        c.setSignedAt(java.time.LocalDateTime.now());
        contractRepository.save(c);

        bed.setStatus(com.ktx.domain.enums.BedStatus.OCCUPIED);
        bedRepository.save(bed);

        redirectAttributes.addFlashAttribute("successMessage", "Gán giường và tạo hợp đồng hoạt động thành công cho sinh viên " + student.getFullName() + "!");
        return "redirect:/student/dashboard";
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
