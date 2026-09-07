package com.ktx.web.admin;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Student;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;
import com.ktx.dto.ViolationForm;
import com.ktx.repository.StudentRepository;
import com.ktx.service.ViolationService;

@Controller
@RequestMapping("/admin/violations")
public class AdminViolationController {

    private final ViolationService violationService;
    private final StudentRepository studentRepository;

    public AdminViolationController(ViolationService violationService, StudentRepository studentRepository) {
        this.violationService = violationService;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public String list(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", violationService.prefilledForm());
        }
        model.addAttribute("students", studentRepository.findAllWithUser());
        model.addAttribute("violations", violationService.listAll());
        model.addAttribute("violationTypes", ViolationType.values());
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("actions", ViolationAction.values());
        model.addAttribute("typePresets", violationService.typePresets());
        model.addAttribute("warnThreshold", violationService.getWarnThreshold());
        model.addAttribute("initialConduct", violationService.getConductInitial());

        page(model, "Báo cáo vi phạm", "Biên bản vi phạm, trừ điểm rèn luyện và chặn đăng ký khi cần");
        return "admin/violations";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ViolationForm form, BindingResult bindingResult,
            Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("form", form);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.form", bindingResult);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu vi phạm chưa hợp lệ");
            return "redirect:/admin/violations";
        }
        try {
            ViolationService.RecordViolationResult result = violationService.recordViolation(form, principal.getName());
            String message = "Đã ghi nhận vi phạm. Điểm rèn luyện: " + result.getPreviousScore() + " → " + result.getCurrentScore() + ".";
            if (result.isBlockedAndSuggestTerminate()) {
                message += " Sinh viên đã bị chặn đăng ký chỗ ở, nên xem xét chấm dứt hợp đồng hiện tại.";
            } else if (result.isWarning()) {
                message += " Điểm đã dưới ngưỡng cảnh cáo.";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
        }
        return "redirect:/admin/violations";
    }

    @PostMapping("/reset-score")
    public String resetScore(@RequestParam("studentId") Long studentId, RedirectAttributes redirectAttributes) {
        try {
            Student student = violationService.resetConductScore(studentId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã reset điểm rèn luyện về " + student.getConductScore() + " cho sinh viên " + student.getStudentCode() + ".");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/violations";
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "violations");
    }
}
