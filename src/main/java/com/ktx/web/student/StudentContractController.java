package com.ktx.web.student;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
public class StudentContractController {

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;

    public StudentContractController(StudentRepository studentRepository,
                                     ContractRepository contractRepository) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
    }

    @GetMapping("/contract")
    public String viewContract(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Hợp đồng lưu trú");
        model.addAttribute("pageSubtitle", "Thông tin hợp đồng và quy chế phòng ở");
        model.addAttribute("activeMenu", "contract");
        return "student/contract/detail";
    }

    @GetMapping("/renewals")
    public String renewals(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Gia hạn hợp đồng");
        model.addAttribute("pageSubtitle", "Nộp đơn xin ở tiếp sang học kỳ mới");
        model.addAttribute("activeMenu", "contract");
        return "student/contract/renewals";
    }

    @PostMapping("/renewals")
    public String submitRenewal(@RequestParam(value = "termMonths", defaultValue = "5") Integer termMonths,
                                @RequestParam(value = "note", required = false) String note,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        if (contracts.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa có hợp đồng phòng ở để gia hạn.");
            return "redirect:/student/renewals";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã gửi đơn xin gia hạn thêm " + termMonths + " tháng thành công! Ban quản lý sẽ xét duyệt trước kỳ mới.");
        return "redirect:/student/renewals";
    }

    @GetMapping("/room-change")
    public String roomChange(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Yêu cầu chuyển phòng");
        model.addAttribute("pageSubtitle", "Đăng ký đổi sang phòng hoặc giường khác");
        model.addAttribute("activeMenu", "contract");
        return "student/contract/room-change";
    }

    @PostMapping("/room-change")
    public String submitRoomChange(@RequestParam("targetRoomType") String targetRoomType,
                                   @RequestParam("reason") String reason,
                                   @RequestParam(value = "preferredBuilding", required = false) String preferredBuilding,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã tiếp nhận đơn xin chuyển phòng! Ban quản lý sẽ rà soát chỗ trống và phản hồi cho bạn qua thông báo.");
        return "redirect:/student/room-change";
    }

    @GetMapping("/return-room")
    public String returnRoom(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("pageTitle", "Yêu cầu trả phòng");
        model.addAttribute("pageSubtitle", "Thủ tục thanh lý hợp đồng và bàn giao chỗ ở");
        model.addAttribute("activeMenu", "contract");
        return "student/contract/return-room";
    }

    @PostMapping("/return-room")
    public String submitReturnRoom(@RequestParam("returnDate") String returnDate,
                                   @RequestParam("reason") String reason,
                                   @RequestParam("bankAccount") String bankAccount,
                                   @RequestParam("bankName") String bankName,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã tiếp nhận yêu cầu trả phòng vào ngày " + returnDate + "! Vui lòng gặp cán bộ quản lý tòa nhà để kiểm kê tài sản và nhận hoàn cọc.");
        return "redirect:/student/return-room";
    }

    private Student getStudent(Principal principal) {
        return studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));
    }
}
