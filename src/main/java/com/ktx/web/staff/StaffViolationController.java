package com.ktx.web.staff;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Student;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.ContractRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.ConductService;

@Controller
@RequestMapping("/staff/violations")
public class StaffViolationController {

    private final ConductService conductService;
    private final ContractRepository contractRepository;
    private final StaffScope staffScope;

    public StaffViolationController(ConductService conductService,
                                  ContractRepository contractRepository,
                                  StaffScope staffScope) {
        this.conductService = conductService;
        this.contractRepository = contractRepository;
        this.staffScope = staffScope;
    }

    @GetMapping
    public String listViolations(Authentication auth, Model model) {
        List<Violation> violations = conductService.getViolationsForStaff(auth);

        model.addAttribute("violations", violations);
        model.addAttribute("pageTitle", "Kỷ luật & Vi phạm");
        model.addAttribute("pageSubtitle", "Theo dõi và lập biên bản vi phạm của sinh viên trong tòa");
        model.addAttribute("activeMenu", "violations");

        return "staff/violations/list";
    }

    @GetMapping("/new")
    public String newViolationForm(Authentication auth, Model model) {
        Long buildingId = staffScope.buildingId(auth)
                .orElseThrow(() -> new AccessDeniedException(StaffScope.DENIED_STAFF));

        List<Contract> contracts = contractRepository.findOccupyingContractsByBuildingId(
                buildingId, OccupyingStatuses.OCCUPYING);
        List<Student> students = contracts.stream().map(Contract::getStudent).distinct().toList();

        model.addAttribute("students", students);
        model.addAttribute("types", ViolationType.values());
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("actions", ViolationAction.values());
        model.addAttribute("pageTitle", "Lập biên bản vi phạm");
        model.addAttribute("pageSubtitle", "Ghi nhận vi phạm sinh viên trong tòa phụ trách");
        model.addAttribute("activeMenu", "violations");

        return "staff/violations/form";
    }

    @PostMapping
    public String recordViolation(@RequestParam("studentId") Long studentId,
                                  @RequestParam("violationType") ViolationType type,
                                  @RequestParam(value = "severity", required = false) ViolationSeverity severity,
                                  @RequestParam(value = "pointsDeducted", required = false) Integer pointsDeducted,
                                  @RequestParam(value = "action", required = false) ViolationAction action,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "occurredAt", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredAt,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        conductService.recordViolation(studentId, auth.getName(), type, severity, pointsDeducted, action,
                description, occurredAt, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận biên bản vi phạm thành công!");
        return "redirect:/staff/violations";
    }
}
