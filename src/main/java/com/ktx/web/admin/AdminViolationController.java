package com.ktx.web.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Student;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.ConductService;

@Controller
@RequestMapping({"/manage/violations", "/admin/violations"})
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'QUAN_LY', 'CAN_BO') or hasAuthority('violation.write')")
public class AdminViolationController {

    private final ConductService conductService;
    private final BuildingRepository buildingRepository;
    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final StaffScope staffScope;

    public AdminViolationController(ConductService conductService,
                                    BuildingRepository buildingRepository,
                                    StudentRepository studentRepository,
                                    @Autowired(required = false) ContractRepository contractRepository,
                                    @Autowired(required = false) StaffScope staffScope) {
        this.conductService = conductService;
        this.buildingRepository = buildingRepository;
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
        this.staffScope = staffScope;
    }

    @GetMapping
    public String listViolations(@RequestParam(value = "buildingId", required = false) Long buildingId,
                                 Authentication auth,
                                 Model model) {
        boolean isStaffScoper = isStaffScoped(auth);
        Long effectiveBuildingId = buildingId;
        List<Building> buildings;

        if (isStaffScoper && staffScope != null && staffScope.buildingId(auth).isPresent()) {
            effectiveBuildingId = staffScope.buildingId(auth).get();
            buildings = buildingRepository.findById(effectiveBuildingId).stream().toList();
        } else {
            buildings = buildingRepository.findAll();
        }

        List<Violation> violations = conductService.getViolationsForAdmin(effectiveBuildingId);

        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", effectiveBuildingId);
        model.addAttribute("violations", violations);
        model.addAttribute("pageTitle", "Kỷ luật & Điểm rèn luyện");
        model.addAttribute("pageSubtitle", "Theo dõi biên bản vi phạm và xét chuẩn điều kiện lưu trú");
        model.addAttribute("activeMenu", "violations");

        return "admin/violations/list";
    }

    @GetMapping("/new")
    public String newViolationForm(Authentication auth, Model model) {
        List<Student> students;
        boolean isStaffScoper = isStaffScoped(auth);
        if (isStaffScoper && staffScope != null && staffScope.buildingId(auth).isPresent() && contractRepository != null) {
            Long bId = staffScope.buildingId(auth).get();
            List<Contract> contracts = contractRepository.findOccupyingContractsByBuildingId(
                    bId, OccupyingStatuses.OCCUPYING);
            students = contracts.stream().map(Contract::getStudent).distinct().toList();
        } else {
            students = studentRepository.findAllWithUser();
        }

        model.addAttribute("students", students);
        model.addAttribute("types", ViolationType.values());
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("actions", ViolationAction.values());
        model.addAttribute("pageTitle", "Lập biên bản vi phạm");
        model.addAttribute("pageSubtitle", "Ghi nhận vi phạm nội quy và trừ điểm rèn luyện của sinh viên");
        model.addAttribute("activeMenu", "violations");

        return "admin/violations/form";
    }

    @PostMapping
    public String recordViolation(@RequestParam("studentId") Long studentId,
                                  @RequestParam("violationType") ViolationType type,
                                  @RequestParam(value = "severity", required = false) ViolationSeverity severity,
                                  @RequestParam(value = "pointsDeducted", required = false) Integer pointsDeducted,
                                  @RequestParam(value = "action", required = false) ViolationAction action,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "occurredAt", required = false)
                                  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime occurredAt,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        conductService.recordViolation(studentId, auth.getName(), type, severity, pointsDeducted, action,
                description, occurredAt, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận vi phạm thành công!");
        return "redirect:" + base();
    }

    @PostMapping("/reset")
    public String resetConductScores(RedirectAttributes redirectAttributes) {
        conductService.resetAllConductScores();
        redirectAttributes.addFlashAttribute("successMessage", "Đã reset điểm rèn luyện của tất cả sinh viên về 100 điểm!");
        return "redirect:" + base();
    }

    private boolean isStaffScoped(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_QUAN_LY".equals(a.getAuthority()));
    }

    private String base() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String uri = sra.getRequest().getRequestURI();
                if (uri != null && uri.startsWith("/manage")) {
                    return "/manage/violations";
                }
            }
        } catch (Exception ignored) {}
        return "/admin/violations";
    }
}
