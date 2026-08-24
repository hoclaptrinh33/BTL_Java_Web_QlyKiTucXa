package com.ktx.web.admin;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.User;
import com.ktx.dto.AdminProfileForm;
import com.ktx.dto.PasswordChangeForm;
import com.ktx.service.AuthService;

@Controller
public class AdminProfileController {

    private final AuthService authService;

    public AdminProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/admin/profile")
    public String viewProfile(Principal principal, Model model) {
        User admin = authService.getAdminProfile(principal.getName());

        AdminProfileForm form = new AdminProfileForm();
        form.setEmail(admin.getEmail());

        model.addAttribute("profileForm", form);
        model.addAttribute("admin", admin);
        model.addAttribute("pageTitle", "Thông tin cá nhân");
        model.addAttribute("activeMenu", "profile");
        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") AdminProfileForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            User admin = authService.getAdminProfile(principal.getName());
            model.addAttribute("admin", admin);
            model.addAttribute("pageTitle", "Thông tin cá nhân");
            model.addAttribute("activeMenu", "profile");
            return "admin/profile";
        }
        try {
            authService.updateAdminProfile(principal.getName(), form);
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            User admin = authService.getAdminProfile(principal.getName());
            model.addAttribute("admin", admin);
            model.addAttribute("pageTitle", "Thông tin cá nhân");
            model.addAttribute("activeMenu", "profile");
            return "admin/profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật email thành công!");
        return "redirect:/admin/profile";
    }

    @GetMapping("/admin/password")
    public String viewPasswordForm(Model model) {
        model.addAttribute("passwordForm", new PasswordChangeForm());
        model.addAttribute("pageTitle", "Đổi mật khẩu");
        model.addAttribute("activeMenu", "password");
        return "admin/password";
    }

    @PostMapping("/admin/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
            BindingResult binding, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (form.getNewPassword() != null && !form.getNewPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Xác nhận mật khẩu mới không khớp");
        }
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "admin/password";
        }
        try {
            authService.changePassword(principal.getName(), form);
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("currentPassword", "invalid", ex.getMessage());
            model.addAttribute("pageTitle", "Đổi mật khẩu");
            model.addAttribute("activeMenu", "password");
            return "admin/password";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/admin/password";
    }
}
