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
import org.springframework.web.bind.annotation.ResponseBody;
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
                                 @RequestParam(value = "page", defaultValue = "0") int page,
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

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), 20);
        org.springframework.data.domain.Page<Violation> violationPage = conductService.getViolationsForAdmin(effectiveBuildingId, pageable);

        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", effectiveBuildingId);
        model.addAttribute("violationPage", violationPage);
        model.addAttribute("violations", violationPage.getContent());
        model.addAttribute("pageTitle", "Kỷ luật & Điểm rèn luyện");
        model.addAttribute("pageSubtitle", "Theo dõi biên bản vi phạm và xét chuẩn điều kiện lưu trú");
        model.addAttribute("activeMenu", "violations");

        return "admin/violations/list";
    }

    @GetMapping("/search-students")
    @ResponseBody
    public List<java.util.Map<String, Object>> searchStudents(
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Authentication auth) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim();
        boolean isStaffScoper = isStaffScoped(auth);
        List<Student> students;
        if (isStaffScoper && staffScope != null && staffScope.buildingId(auth).isPresent() && contractRepository != null) {
            Long bId = staffScope.buildingId(auth).get();
            students = contractRepository.searchOccupyingByBuildingId(
                    bId, OccupyingStatuses.OCCUPYING, kw, org.springframework.data.domain.PageRequest.of(0, 15));
        } else {
            students = studentRepository.searchByKeyword(kw, org.springframework.data.domain.PageRequest.of(0, 15));
        }

        if (students.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = students.stream().map(Student::getId).toList();
        java.util.Map<Long, String> roomMap = new java.util.HashMap<>();
        if (contractRepository != null) {
            List<Contract> contracts = contractRepository.findOccupyingByStudentIdsWithDetails(studentIds, OccupyingStatuses.OCCUPYING);
            for (Contract c : contracts) {
                if (c.getBed() != null && c.getBed().getRoom() != null) {
                    String bldg = c.getBed().getRoom().getBuilding() != null ? c.getBed().getRoom().getBuilding().getCode() : "";
                    roomMap.put(c.getStudent().getId(), "P." + c.getBed().getRoom().getRoomNumber() + (!bldg.isEmpty() ? " (" + bldg + ")" : ""));
                }
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
            map.put("room", roomMap.getOrDefault(s.getId(), "Chưa xếp phòng"));
            return map;
        }).toList();
    }

    @GetMapping("/new")
    public String newViolationForm(
            @RequestParam(value = "studentId", required = false) Long preselectedStudentId,
            Authentication auth, Model model) {
        if (preselectedStudentId != null) {
            studentRepository.findById(preselectedStudentId).ifPresent(s -> {
                model.addAttribute("selectedStudent", s);
                if (contractRepository != null) {
                    List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(s.getId(), OccupyingStatuses.OCCUPYING);
                    if (!contracts.isEmpty() && contracts.get(0).getBed() != null && contracts.get(0).getBed().getRoom() != null) {
                        String bldg = contracts.get(0).getBed().getRoom().getBuilding() != null ? contracts.get(0).getBed().getRoom().getBuilding().getCode() : "";
                        model.addAttribute("selectedStudentRoom", "P." + contracts.get(0).getBed().getRoom().getRoomNumber() + (!bldg.isEmpty() ? " (" + bldg + ")" : ""));
                    }
                }
            });
        }

        model.addAttribute("students", List.of());
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
