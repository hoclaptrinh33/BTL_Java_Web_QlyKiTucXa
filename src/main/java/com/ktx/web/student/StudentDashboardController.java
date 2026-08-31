package com.ktx.web.student;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.RoomApplicationService;
import com.ktx.service.DashboardService;

@Controller
public class StudentDashboardController {

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public StudentDashboardController(StudentRepository studentRepository,
                                      ContractRepository contractRepository,
                                      RoomApplicationRepository roomApplicationRepository,
                                      InvoiceRepository invoiceRepository) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
        this.roomApplicationRepository = roomApplicationRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Principal principal, Model model) {
        Student student = studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));

        // 1. Lấy hợp đồng đang hoạt động (ở phòng nào)
        List<Contract> activeContracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = activeContracts.isEmpty() ? null : activeContracts.get(0);

        // 2. Lấy danh sách bạn cùng phòng
        List<Contract> roommates = List.of();
        if (activeContract != null) {
            Long roomId = activeContract.getBed().getRoom().getId();
            roommates = contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING).stream()
                    .filter(c -> !c.getStudent().getId().equals(student.getId()))
                    .collect(Collectors.toList());
        }

        // 3. Lấy lịch sử đăng ký gần đây (Giới hạn 5 đơn)
        List<RoomApplication> applications = roomApplicationRepository.findByStudentIdOrderByPeriodOpenAtDesc(student.getId());
        List<RoomApplication> recentApplications = applications.stream().limit(5).collect(Collectors.toList());

        // 4. Lấy hóa đơn của sinh viên
        List<Invoice> invoices = invoiceRepository.findByStudentIdOrderByDueDateDesc(student.getId());
        List<Invoice> unpaidInvoices = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.OVERDUE)
                .collect(Collectors.toList());
        
        BigDecimal totalUnpaidAmount = unpaidInvoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Nạp dữ liệu vào Model
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Cổng thông tin sinh viên");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("student", student);
        model.addAttribute("activeContract", activeContract);
        model.addAttribute("roommates", roommates);
        model.addAttribute("recentApplications", recentApplications);
        model.addAttribute("invoices", invoices.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("unpaidCount", unpaidInvoices.size());
        model.addAttribute("totalUnpaidAmount", totalUnpaidAmount);
        model.addAttribute("totalAppsCount", applications.size());
        model.addAttribute("initials", DashboardService.initials(student.getFullName()));

        return "student/dashboard";
    }
}
