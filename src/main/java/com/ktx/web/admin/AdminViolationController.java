package com.ktx.web.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Building;
import com.ktx.domain.Student;
import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.ConductService;

@Controller
@RequestMapping("/admin/violations")
public class AdminViolationController {

    private final ConductService conductService;
    private final BuildingRepository buildingRepository;
    private final StudentRepository studentRepository;

    public AdminViolationController(ConductService conductService,
                                    BuildingRepository buildingRepository,
                                    StudentRepository studentRepository) {
        this.conductService = conductService;
        this.buildingRepository = buildingRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public String listViolations(@RequestParam(value = "buildingId", required = false) Long buildingId,
                                 Model model) {
        List<Building> buildings = buildingRepository.findAll();
        List<Violation> violations = conductService.getViolationsForAdmin(buildingId);

        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", buildingId);
        model.addAttribute("violations", violations);
        model.addAttribute("pageTitle", "Kỷ luật & Điểm rèn luyện");
        model.addAttribute("pageSubtitle", "Theo dõi biên bản vi phạm và xét chuẩn điều kiện lưu trú");
        model.addAttribute("activeMenu", "violations");

        return "admin/violations/list";
    }

    @GetMapping("/new")
    public String newViolationForm(Model model) {
        List<Student> students = studentRepository.findAllWithUser();

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
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredAt,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        conductService.recordViolation(studentId, auth.getName(), type, severity, pointsDeducted, action,
                description, occurredAt, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận vi phạm thành công!");
        return "redirect:/admin/violations";
    }

    @PostMapping("/reset")
    public String resetConductScores(RedirectAttributes redirectAttributes) {
        conductService.resetAllConductScores();
        redirectAttributes.addFlashAttribute("successMessage", "Đã reset điểm rèn luyện của tất cả sinh viên về 100 điểm!");
        return "redirect:/admin/violations";
    }
}
