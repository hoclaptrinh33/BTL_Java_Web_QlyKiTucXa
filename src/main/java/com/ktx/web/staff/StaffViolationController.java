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

    @GetMapping("/search-students")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<java.util.Map<String, Object>> searchStudents(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Authentication auth) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Long buildingId = staffScope.buildingId(auth)
                .orElseThrow(() -> new AccessDeniedException(StaffScope.DENIED_STAFF));

        List<Student> students = contractRepository.searchOccupyingByBuildingId(
                buildingId, OccupyingStatuses.OCCUPYING, keyword.trim(), org.springframework.data.domain.PageRequest.of(0, 15));

        if (students.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        java.util.Map<Long, String> roomMap = new java.util.HashMap<>();
        List<Contract> contracts = contractRepository.findOccupyingByStudentIdsWithDetails(studentIds, OccupyingStatuses.OCCUPYING);
        for (Contract c : contracts) {
            if (c.getBed() != null && c.getBed().getRoom() != null) {
                String bldg = c.getBed().getRoom().getBuilding() != null ? c.getBed().getRoom().getBuilding().getCode() : "";
                roomMap.put(c.getStudent().getId(), "P." + c.getBed().getRoom().getRoomNumber() + (!bldg.isEmpty() ? " (" + bldg + ")" : ""));
            }
        }

        return students.stream().map(s -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("fullName", s.getFullName());
            map.put("studentCode", s.getStudentCode());
            map.put("conductScore", s.getConductScore() != null ? s.getConductScore() : 100);
            map.put("facultyCode", s.getFacultyCode() != null ? s.getFacultyCode() : "");
            map.put("classCode", s.getClassCode() != null ? s.getClassCode() : "");
            map.put("room", roomMap.getOrDefault(s.getId(), "Phòng ở tòa"));
            return map;
        }).toList();
    }

    @GetMapping("/new")
    public String newViolationForm(
            @RequestParam(value = "studentId", required = false) Long preselectedStudentId,
            Authentication auth, Model model) {
        Long buildingId = staffScope.buildingId(auth)
                .orElseThrow(() -> new AccessDeniedException(StaffScope.DENIED_STAFF));

        if (preselectedStudentId != null) {
            List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(preselectedStudentId, OccupyingStatuses.OCCUPYING);
            if (!contracts.isEmpty()) {
                Student s = contracts.get(0).getStudent();
                model.addAttribute("selectedStudent", s);
                if (contracts.get(0).getBed() != null && contracts.get(0).getBed().getRoom() != null) {
                    String bldg = contracts.get(0).getBed().getRoom().getBuilding() != null ? contracts.get(0).getBed().getRoom().getBuilding().getCode() : "";
                    model.addAttribute("selectedStudentRoom", "P." + contracts.get(0).getBed().getRoom().getRoomNumber() + (!bldg.isEmpty() ? " (" + bldg + ")" : ""));
                }
            }
        }

        model.addAttribute("students", List.of());
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
                                  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime occurredAt,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        conductService.recordViolation(studentId, auth.getName(), type, severity, pointsDeducted, action,
                description, occurredAt, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận biên bản vi phạm thành công!");
        return "redirect:/staff/violations";
    }
}
