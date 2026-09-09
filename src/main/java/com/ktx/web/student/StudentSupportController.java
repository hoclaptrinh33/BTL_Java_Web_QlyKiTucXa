package com.ktx.web.student;

import java.security.Principal;
import java.util.List;

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

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Yêu cầu sửa chữa & Sự cố");
        model.addAttribute("pageSubtitle", "Báo cáo sự cố điện nước, đồ đạc trong phòng");
        model.addAttribute("activeMenu", "support");
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
        model.addAttribute("activeMenu", "support");
        return "student/tickets/form";
    }

    @PostMapping("/tickets")
    public String submitTicket(@RequestParam("category") String category,
                               @RequestParam("title") String title,
                               @RequestParam("description") String description,
                               @RequestParam(value = "severity", defaultValue = "NORMAL") String severity,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã tạo yêu cầu sửa chữa #" + System.currentTimeMillis() % 10000 + " thành công! Nhân viên kỹ thuật sẽ kiểm tra trong 24h làm việc.");
        return "redirect:/student/tickets";
    }

    @PostMapping("/tickets/{id}/close")
    public String closeTicket(@PathVariable("id") Long id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
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
}
