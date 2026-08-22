package com.ktx.web.staff;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Staff;
import com.ktx.dto.PasswordChangeForm;
import com.ktx.dto.StaffProfileForm;
import com.ktx.service.AuthService;

@Controller
public class StaffProfileController {

    private final AuthService authService;

    public StaffProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/staff/profile")
    public String viewProfile(Principal principal, Model model) {
        Staff staff = authService.getStaffProfile(principal.getName());

        StaffProfileForm form = new StaffProfileForm();
        form.setFullName(staff.getFullName());
        form.setPhone(staff.getPhone());

        model.addAttribute("profileForm", form);
        model.addAttribute("staff", staff);
        model.addAttribute("pageTitle", "Thông tin cá nhân");
        model.addAttribute("activeMenu", "profile");
        return "staff/profile";
    }

    @PostMapping("/staff/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") StaffProfileForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            Staff staff = authService.getStaffProfile(principal.getName());
            model.addAttribute("staff", staff);
            model.addAttribute("pageTitle", "Thông tin cá nhân");
            model.addAttribute("activeMenu", "profile");
            return "staff/profile";
        }
        authService.updateStaffProfile(principal.getName(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        return "redirect:/staff/profile";
    }

    @GetMapping("/staff/password")
    public String viewPasswordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("pageTitle", "Đổi mật khẩu");
        model.addAttribute("activeMenu", "password");
        return "staff/password";
    }

    @PostMapping("/staff/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (form.getNewPassword() != null && !form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Xác nhận mật khẩu mới không khớp");
        }
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "staff/password";
        }
        try {
            authService.changePassword(principal.getName(), form);
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("currentPassword", "invalid", ex.getMessage());
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "staff/password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/staff/password";
    }
}
