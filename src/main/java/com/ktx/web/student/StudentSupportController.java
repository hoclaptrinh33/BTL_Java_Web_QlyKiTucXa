package com.ktx.web.student;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
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
import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.Student;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.TicketPriority;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.ConductService;
import com.ktx.service.RoomApplicationService;
import com.ktx.service.TicketService;

@Controller
@RequestMapping("/student")
public class StudentSupportController {

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final TicketService ticketService;
    private final ConductService conductService;

    public StudentSupportController(StudentRepository studentRepository,
                                    ContractRepository contractRepository,
                                    TicketService ticketService,
                                    ConductService conductService) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
        this.ticketService = ticketService;
        this.conductService = conductService;
    }

    @GetMapping("/tickets")
    public String tickets(Principal principal, Model model) {
        Student student = getStudent(principal);
        Contract activeContract = activeContract(student);

        List<MaintenanceTicket> tickets = ticketService.getTicketsForStudent(student.getId());
        long countWaiting = tickets.stream().filter(t -> t.getStatus() == TicketStatus.OPEN).count();
        long countInProgress = tickets.stream().filter(t -> t.getStatus() == TicketStatus.IN_PROGRESS).count();
        long countCompleted = tickets.stream()
                .filter(t -> t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED)
                .count();

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
        Contract activeContract = activeContract(student);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("priorities", TicketPriority.values());
        model.addAttribute("pageTitle", "Tạo phiếu báo hỏng thiết bị");
        model.addAttribute("pageSubtitle", "Gửi thông tin sự cố tới ban quản lý và đội kỹ thuật");
        model.addAttribute("activeMenu", "tickets-new");
        return "student/tickets/form";
    }

    @PostMapping("/tickets")
    public String submitTicket(@RequestParam(value = "roomId", required = false) Long roomId,
                               @RequestParam("title") String title,
                               @RequestParam("description") String description,
                               @RequestParam(value = "priority", required = false) TicketPriority priority,
                               @RequestParam(value = "severity", required = false) String severityFallback,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        Contract contract = activeContract(student);
        if (contract == null) {
            throw new AccessDeniedException("Sinh viên chưa có hợp đồng phòng hợp lệ");
        }

        Long targetRoomId = roomId != null ? roomId : contract.getBed().getRoom().getId();
        if (!contract.getBed().getRoom().getId().equals(targetRoomId)) {
            throw new AccessDeniedException("Sinh viên chỉ được tạo ticket cho phòng của mình");
        }

        TicketPriority effectivePriority = priority;
        if (effectivePriority == null && severityFallback != null) {
            try {
                if ("NORMAL".equalsIgnoreCase(severityFallback)) {
                    effectivePriority = TicketPriority.LOW;
                } else if ("URGENT".equalsIgnoreCase(severityFallback)) {
                    effectivePriority = TicketPriority.MEDIUM;
                } else if ("CRITICAL".equalsIgnoreCase(severityFallback)) {
                    effectivePriority = TicketPriority.HIGH;
                } else {
                    effectivePriority = TicketPriority.valueOf(severityFallback.toUpperCase());
                }
            } catch (Exception ignored) {
                effectivePriority = TicketPriority.MEDIUM;
            }
        }

        ticketService.createTicket(student.getId(), targetRoomId, title, description, effectivePriority);
        redirectAttributes.addFlashAttribute("successMessage", "Gửi phiếu báo hỏng thiết bị thành công!");
        return "redirect:/student/tickets";
    }

    @PostMapping("/tickets/{id}/close")
    public String closeTicket(@PathVariable("id") Long id,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        ticketService.closeTicketByStudent(id, student.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Đã đóng ticket thành công!");
        return "redirect:/student/tickets";
    }

    @GetMapping("/violations")
    public String violations(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Violation> violations = conductService.getViolationsForStudent(student.getId());

        model.addAttribute("student", student);
        model.addAttribute("violations", violations);
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
