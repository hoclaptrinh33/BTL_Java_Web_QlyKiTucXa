package com.ktx.web.admin;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.AdminAccountForm;
import com.ktx.repository.UserRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(Model model) {
        List<User> admins = userRepository.findByRoleIn(List.of(Role.ADMIN));
        long enabledCount = admins.stream().filter(u -> Boolean.TRUE.equals(u.getEnabled())).count();

        model.addAttribute("pageTitle", "Tài khoản quản trị");
        model.addAttribute("pageSubtitle", "Quản lý và cấp quyền tài khoản Admin hệ thống");
        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("admins", admins);
        model.addAttribute("enabledCount", enabledCount);
        return "admin/accounts/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Tạo tài khoản quản trị");
        model.addAttribute("pageSubtitle", "Thêm tài khoản quản trị viên hệ thống mới");
        model.addAttribute("activeMenu", "accounts");
        model.addAttribute("accountForm", new AdminAccountForm());
        return "admin/accounts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("accountForm") AdminAccountForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (form.getPassword() == null || form.getPassword().trim().length() < 8) {
            bindingResult.rejectValue("password", "size", "Mật khẩu bắt buộc từ 8 ký tự trở lên");
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã được sử dụng");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo tài khoản quản trị");
            model.addAttribute("pageSubtitle", "Thêm tài khoản quản trị viên hệ thống mới");
            model.addAttribute("activeMenu", "accounts");
            return "admin/accounts/form";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            User user = new User();
            user.setUsername(form.getUsername().trim());
            user.setEmail(form.getEmail().trim());
            user.setPasswordHash(passwordEncoder.encode(form.getPassword().trim()));
            user.setRole(Role.ADMIN);
            user.setEnabled(form.isEnabled());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản quản trị viên thành công!");
            return "redirect:/admin/accounts";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Tạo tài khoản quản trị");
            model.addAttribute("pageSubtitle", "Thêm tài khoản quản trị viên hệ thống mới");
            model.addAttribute("activeMenu", "accounts");
            return "admin/accounts/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản quản trị"));

            if (user.getRole() != Role.ADMIN) {
                throw new BusinessException("Tài khoản không phải quản trị viên hệ thống");
            }

            if (Boolean.TRUE.equals(user.getEnabled())) {
                long activeAdmins = userRepository.findByRoleIn(List.of(Role.ADMIN)).stream()
                        .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                        .count();
                if (activeAdmins <= 1) {
                    throw new BusinessException("Không thể khóa tài khoản quản trị viên hệ thống cuối cùng");
                }
            }

            user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            String statusStr = Boolean.TRUE.equals(user.getEnabled()) ? "Kích hoạt" : "Khóa";
            redirectAttributes.addFlashAttribute("successMessage", statusStr + " tài khoản quản trị thành công!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));

            if (user.getRole() != Role.ADMIN) {
                throw new BusinessException("Chỉ xóa tài khoản quản trị viên hệ thống tại màn này");
            }

            long activeAdmins = userRepository.findByRoleIn(List.of(Role.ADMIN)).stream()
                    .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                    .count();
            if (activeAdmins <= 1) {
                throw new BusinessException("Không thể xóa tài khoản quản trị viên hệ thống cuối cùng");
            }

            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản quản trị viên!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (newPassword == null || newPassword.trim().length() < 8) {
                throw new BusinessException("Mật khẩu phải từ 8 ký tự trở lên");
            }
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản"));
            user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}
