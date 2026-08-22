package com.ktx.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ktx.dto.StudentRow;
import com.ktx.service.StudentService;

@Controller
public class AdminStudentController {

    private final StudentService studentService;

    public AdminStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/admin/students")
    public String list(@RequestParam(name = "stay", defaultValue = StudentService.STAY_ALL) String stay, Model model) {
        var students = studentService.list(stay);
        var all = StudentService.STAY_ALL.equals(stay) ? students : studentService.list(StudentService.STAY_ALL);
        model.addAttribute("pageTitle", "Sinh viên");
        model.addAttribute("pageSubtitle", "Danh sách sinh viên và người đang ở KTX");
        model.addAttribute("activeMenu", "students");
        model.addAttribute("students", students);
        model.addAttribute("stay", stay);
        model.addAttribute("occupyingCount", all.stream().filter(StudentRow::isOccupying).count());
        model.addAttribute("totalCount", all.size());
        return "admin/students/list";
    }
}
