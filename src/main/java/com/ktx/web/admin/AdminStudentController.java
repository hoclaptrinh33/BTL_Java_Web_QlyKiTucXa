package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ktx.service.StudentService;

import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('student.read')")
public class AdminStudentController {

    private final StudentService studentService;

    public AdminStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping({"/manage/students", "/admin/students"})
    public String list(@RequestParam(name = "stay", defaultValue = StudentService.STAY_ALL) String stay,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        var studentPage = studentService.page(stay, org.springframework.data.domain.PageRequest.of(Math.max(page, 0), 40));
        if (studentPage == null) {
            studentPage = org.springframework.data.domain.Page.empty();
        }
        model.addAttribute("pageTitle", "Sinh viên");
        model.addAttribute("pageSubtitle", "Danh sách sinh viên và người đang ở KTX");
        model.addAttribute("activeMenu", "students");
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("studentPage", studentPage);
        model.addAttribute("stay", stay);
        model.addAttribute("occupyingCount", studentService.occupyingCount());
        model.addAttribute("totalCount", studentPage.getTotalElements());
        return "admin/students/list";
    }
}
