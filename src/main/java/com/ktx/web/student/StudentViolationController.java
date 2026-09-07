package com.ktx.web.student;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Student;
import com.ktx.repository.StudentRepository;
import com.ktx.service.ViolationService;

@Controller
@RequestMapping("/student/violations")
public class StudentViolationController {

    private final StudentRepository studentRepository;
    private final ViolationService violationService;

    public StudentViolationController(StudentRepository studentRepository, ViolationService violationService) {
        this.studentRepository = studentRepository;
        this.violationService = violationService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Student student = studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin sinh viên"));
        int warnThreshold = violationService.getWarnThreshold();
        int conductScore = student.getConductScore() == null ? 0 : student.getConductScore();

        model.addAttribute("student", student);
        model.addAttribute("violations", violationService.listByStudent(student.getId()));
        model.addAttribute("warnThreshold", warnThreshold);
        model.addAttribute("isWarning", conductScore < warnThreshold);
        page(model, "Vi phạm & điểm rèn luyện", "Theo dõi biên bản vi phạm của bạn (chỉ đọc)");
        return "student/violations";
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "violations");
    }
}
