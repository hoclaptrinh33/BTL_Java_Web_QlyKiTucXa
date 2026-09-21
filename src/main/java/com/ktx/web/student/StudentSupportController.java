package com.ktx.web.student;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        Contract activeContract = activeContract(student);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("tickets", List.of());
        model.addAttribute("countWaiting", 0L);
        model.addAttribute("countInProgress", 0L);
        model.addAttribute("countCompleted", 0L);
        model.addAttribute("pageTitle", "Yêu cầu sửa chữa & Sự cố");
        model.addAttribute("pageSubtitle", "Báo cáo sự cố điện nước, đồ đạc trong phòng");
        model.addAttribute("activeMenu", "tickets");
        return "student/tickets/list";
    }

    @GetMapping("/tickets/new")
    public String newTicket(Principal principal, Model model) {
        Student student = getStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract(student));
        model.addAttribute("pageTitle", "Tạo phiếu báo hỏng thiết bị");
        model.addAttribute("pageSubtitle", "Gửi thông tin sự cố tới ban quản lý và đội kỹ thuật");
        model.addAttribute("activeMenu", "tickets-new");
        return "student/tickets/form";
    }

    @PostMapping("/tickets")
    public String submitTicket(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Chức năng sự cố/sửa chữa chưa triển khai (module ops). Phiếu chưa được lưu.");
        return "redirect:/student/tickets";
    }

    @PostMapping("/tickets/{id}/close")
    public String closeTicket(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Chức năng sự cố/sửa chữa chưa triển khai (module ops). Phiếu #" + id + " chưa được cập nhật.");
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

    private Contract activeContract(Student student) {
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        return contracts.isEmpty() ? null : contracts.get(0);
    }
}
