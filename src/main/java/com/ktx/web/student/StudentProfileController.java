package com.ktx.web.student;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Student;
import com.ktx.dto.PasswordChangeForm;
import com.ktx.dto.StudentProfileForm;
import com.ktx.service.AuthService;
import com.ktx.service.StudentProfileService;

@Controller
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final AuthService authService;

    public StudentProfileController(StudentProfileService studentProfileService, AuthService authService) {
        this.studentProfileService = studentProfileService;
        this.authService = authService;
    }

    @GetMapping("/student/profile")
    public String viewProfile(Principal principal, Model model) {
        Student student = studentProfileService.getStudentByUsername(principal.getName());

        StudentProfileForm form = new StudentProfileForm();
        form.setPhone(student.getPhone());
        form.setEmergencyName(student.getEmergencyName());
        form.setEmergencyPhone(student.getEmergencyPhone());
        form.setHometown(student.getHometown());

        model.addAttribute("profileForm", form);
        model.addAttribute("student", student);
        model.addAttribute("pageTitle", "Thông tin cá nhân");
        model.addAttribute("activeMenu", "profile");
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") StudentProfileForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            Student student = studentProfileService.getStudentByUsername(principal.getName());
            model.addAttribute("student", student);
            model.addAttribute("pageTitle", "Thông tin cá nhân");
            model.addAttribute("activeMenu", "profile");
            return "student/profile";
        }
        studentProfileService.updateStudentProfile(principal.getName(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        return "redirect:/student/profile";
    }

    @GetMapping("/student/password")
    public String viewPasswordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("pageTitle", "Đổi mật khẩu");
        model.addAttribute("activeMenu", "password");
        return "student/password";
    }

    @PostMapping("/student/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (form.getNewPassword() != null && !form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Xác nhận mật khẩu mới không khớp");
        }
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "student/password";
        }
        try {
            authService.changePassword(principal.getName(), form);
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("currentPassword", "invalid", ex.getMessage());
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "student/password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/student/password";
    }
}
