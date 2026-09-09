package com.ktx.web.student;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Student;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.RoomApplicationService;

@Controller
@RequestMapping("/student")
public class StudentSupportController {

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final Map<String, List<TicketItem>> userTickets = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(3000);

    public StudentSupportController(StudentRepository studentRepository,
                                    ContractRepository contractRepository) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping("/tickets")
    public String tickets(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        List<TicketItem> tickets = getOrCreateTickets(principal.getName());
        long countWaiting = tickets.stream().filter(t -> "WAITING".equals(t.getStatus())).count();
        long countInProgress = tickets.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        long countCompleted = tickets.stream().filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus())).count();

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("tickets", tickets);
        model.addAttribute("countWaiting", countWaiting);
        model.addAttribute("countInProgress", countInProgress);
        model.addAttribute("countCompleted", countCompleted);

        model.addAttribute("pageTitle", "Yêu cầu sửa chữa & Sự cố");
        model.addAttribute("pageSubtitle", "Báo cáo sự cố điện nước, đồ đạc trong phòng");
        model.addAttribute("activeMenu", "tickets");
        return "student/tickets/list";
    }

    @GetMapping("/tickets/new")
    public String newTicket(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Tạo phiếu báo hỏng thiết bị");
        model.addAttribute("pageSubtitle", "Gửi thông tin sự cố tới ban quản lý và đội kỹ thuật");
        model.addAttribute("activeMenu", "tickets-new");
        return "student/tickets/form";
    }

    @PostMapping("/tickets")
    public String submitTicket(@RequestParam("category") String category,
                               @RequestParam("title") String title,
                               @RequestParam("description") String description,
                               @RequestParam(value = "severity", defaultValue = "NORMAL") String severity,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        long id = nextId.incrementAndGet();
        String code = "#TK-" + id;
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String catName;
        String subCat = title;
        String icon;
        String color;

        switch (category) {
            case "ELECTRIC":
                catName = "Điện & Đèn chiếu sáng";
                icon = "bi-lightning-charge";
                color = "text-warning";
                break;
            case "AC":
                catName = "Điều hòa nhiệt độ";
                icon = "bi-snow";
                color = "text-info";
                break;
            case "WATER":
                catName = "Nước & Thiết bị vệ sinh";
                icon = "bi-water";
                color = "text-info";
                break;
            case "DOOR":
                catName = "Khóa cửa & Đồ gỗ";
                icon = "bi-door-closed";
                color = "text-secondary";
                break;
            default:
                catName = "Cơ sở vật chất khác";
                icon = "bi-tools";
                color = "text-primary";
                break;
        }

        String sevName;
        String sevClass;
        switch (severity) {
            case "CRITICAL":
                sevName = "Rất khẩn cấp";
                sevClass = "bg-danger-subtle text-danger";
                break;
            case "URGENT":
                sevName = "Khẩn cấp";
                sevClass = "bg-warning-subtle text-warning";
                break;
            default:
                sevName = "Bình thường";
                sevClass = "bg-secondary-subtle text-secondary";
                break;
        }

        TicketItem newTicket = new TicketItem(
                id, code, catName, subCat, icon, color, description,
                sevName, sevClass, dateStr, "WAITING", "Chờ tiếp nhận", "is-peach", "Đang đợi thợ"
        );

        List<TicketItem> list = getOrCreateTickets(principal.getName());
        list.add(0, newTicket);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã tạo yêu cầu sửa chữa " + code + " thành công! Nhân viên kỹ thuật sẽ kiểm tra trong 24h làm việc.");
        return "redirect:/student/tickets";
    }

    @PostMapping("/tickets/{id}/close")
    public String closeTicket(@PathVariable("id") Long id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        List<TicketItem> list = getOrCreateTickets(principal.getName());
        for (TicketItem item : list) {
            if (item.getId().equals(id)) {
                item.setStatus("CLOSED");
                item.setStatusLabel("Đã đóng");
                item.setStatusClass("is-muted");
                item.setActionNote("Đã hoàn tất");
                break;
            }
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã xác nhận hoàn tất sửa chữa và đóng yêu cầu hỗ trợ.");
        return "redirect:/student/tickets";
    }

    @GetMapping("/violations")
    public String violations(Principal principal, Model model) {
        Student student = getStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("pageTitle", "Vi phạm & Điểm rèn luyện");
        model.addAttribute("pageSubtitle", "Lịch sử kỷ luật và quy chế lưu trú ký túc xá");
        model.addAttribute("activeMenu", "violations");
        return "student/violations/list";
    }

    private Student getStudent(Principal principal) {
        return studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));
    }

    private List<TicketItem> getOrCreateTickets(String username) {
        return userTickets.computeIfAbsent(username, u -> {
            List<TicketItem> list = new ArrayList<>();
            list.add(new TicketItem(
                    2041L, "#TK-2041", "Điện & Chiếu sáng", "Bóng tuýp LED trần",
                    "bi-lightning-charge", "text-warning",
                    "Bóng đèn trần phòng nhấp nháy liên tục rồi tắt hẳn, cần thay bóng mới.",
                    "Khẩn cấp", "bg-warning-subtle text-warning",
                    "24/08/2026", "WAITING", "Chờ tiếp nhận", "is-peach", "Đang đợi thợ"
            ));
            list.add(new TicketItem(
                    1988L, "#TK-1988", "Nước & Thiết bị vệ sinh", "Vòi sen phòng tắm",
                    "bi-water", "text-info",
                    "Vòi hoa sen bị rỉ nước liên tục gây hao phí nước sạch.",
                    "Bình thường", "bg-secondary-subtle text-secondary",
                    "18/08/2026", "IN_PROGRESS", "Đang xử lý", "is-violet", "Kỹ thuật đang kiểm tra"
            ));
            list.add(new TicketItem(
                    1850L, "#TK-1850", "Khóa cửa & Đồ gỗ", "Khóa tay gạt cửa chính",
                    "bi-door-closed", "text-secondary",
                    "Khóa cửa ra vào bị kẹt ổ khóa không cắm chìa vào được.",
                    "Rất khẩn cấp", "bg-danger-subtle text-danger",
                    "10/08/2026", "RESOLVED", "Đã sửa xong", "is-mint", null
            ));
            return list;
        });
    }

    public static class TicketItem {
        private Long id;
        private String code;
        private String category;
        private String subCategory;
        private String categoryIcon;
        private String categoryColor;
        private String description;
        private String severity;
        private String severityClass;
        private String createdAt;
        private String status;
        private String statusLabel;
        private String statusClass;
        private String actionNote;

        public TicketItem(Long id, String code, String category, String subCategory, String categoryIcon,
                          String categoryColor, String description, String severity, String severityClass,
                          String createdAt, String status, String statusLabel, String statusClass, String actionNote) {
            this.id = id;
            this.code = code;
            this.category = category;
            this.subCategory = subCategory;
            this.categoryIcon = categoryIcon;
            this.categoryColor = categoryColor;
            this.description = description;
            this.severity = severity;
            this.severityClass = severityClass;
            this.createdAt = createdAt;
            this.status = status;
            this.statusLabel = statusLabel;
            this.statusClass = statusClass;
            this.actionNote = actionNote;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getCategory() { return category; }
        public String getSubCategory() { return subCategory; }
        public String getCategoryIcon() { return categoryIcon; }
        public String getCategoryColor() { return categoryColor; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public String getSeverityClass() { return severityClass; }
        public String getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getStatusClass() { return statusClass; }
        public void setStatusClass(String statusClass) { this.statusClass = statusClass; }
        public String getActionNote() { return actionNote; }
        public void setActionNote(String actionNote) { this.actionNote = actionNote; }
    }
}
