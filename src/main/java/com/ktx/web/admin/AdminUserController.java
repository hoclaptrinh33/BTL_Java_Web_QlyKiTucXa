package com.ktx.web.admin;

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

import com.ktx.dto.BqlUserForm;
import com.ktx.domain.enums.Role;
import com.ktx.service.AuditLogService;
import com.ktx.service.BuildingService;
import com.ktx.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final BuildingService buildingService;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminUserController(UserService userService, BuildingService buildingService,
                              @org.springframework.beans.factory.annotation.Autowired(required = false) AuditLogService auditLogService) {
        this.userService = userService;
        this.buildingService = buildingService;
        this.auditLogService = auditLogService;
    }

    public AdminUserController(UserService userService, BuildingService buildingService) {
        this(userService, buildingService, null);
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Người dùng");
        model.addAttribute("pageSubtitle", "Quản lý tài khoản Admin và Cán bộ tòa");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("users", userService.listBqlUsers());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Thêm người dùng");
        model.addAttribute("pageSubtitle", "Tạo tài khoản Ban quản lý mới");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("userForm", new BqlUserForm());
        model.addAttribute("buildings", buildingService.listAll());
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") BqlUserForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {

        if (form.getPassword() == null || form.getPassword().trim().length() < 8) {
            bindingResult.rejectValue("password", "size", "Mật khẩu bắt buộc từ 8 ký tự trở lên");
        }

        if (form.getRole() == Role.STAFF && form.getAssignedBuildingId() == null) {
            bindingResult.rejectValue("assignedBuildingId", "required", "Cán bộ quản lý tòa nhà (STAFF) bắt buộc phải chọn tòa nhà");
        }

        if (form.getRole() == Role.STAFF && (form.getFullName() == null || form.getFullName().trim().isEmpty())) {
            bindingResult.rejectValue("fullName", "required", "Họ tên cán bộ không được để trống");
        }

        if (userService.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã được sử dụng");
        }

        if (userService.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm người dùng");
            model.addAttribute("pageSubtitle", "Tạo tài khoản Ban quản lý mới");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("buildings", buildingService.listAll());
            return "admin/users/form";
        }

        try {
            userService.createBqlUser(form);
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_CREATE", "USER", form.getUsername(),
                        "Tạo tài khoản Ban quản lý: " + form.getUsername() + " (" + form.getRole() + ")", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_CREATE", "USER", form.getUsername(),
                        "Lỗi tạo tài khoản Ban quản lý: " + ex.getMessage(), "FAILURE");
            }
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Thêm người dùng");
            model.addAttribute("pageSubtitle", "Tạo tài khoản Ban quản lý mới");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("buildings", buildingService.listAll());
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        BqlUserForm form = userService.getBqlUserById(id);
        model.addAttribute("pageTitle", "Sửa người dùng");
        model.addAttribute("pageSubtitle", "Cập nhật tài khoản Ban quản lý");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("userForm", form);
        model.addAttribute("buildings", buildingService.listAll());
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id, @Valid @ModelAttribute("userForm") BqlUserForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (form.getRole() == Role.STAFF && form.getAssignedBuildingId() == null) {
            bindingResult.rejectValue("assignedBuildingId", "required", "Cán bộ quản lý tòa nhà (STAFF) bắt buộc phải chọn tòa nhà");
        }

        if (form.getRole() == Role.STAFF && (form.getFullName() == null || form.getFullName().trim().isEmpty())) {
            bindingResult.rejectValue("fullName", "required", "Họ tên cán bộ không được để trống");
        }

        BqlUserForm current = userService.getBqlUserById(id);
        if (!current.getUsername().equalsIgnoreCase(form.getUsername()) && userService.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã được sử dụng");
        }

        if (!current.getEmail().equalsIgnoreCase(form.getEmail()) && userService.existsByEmail(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "Email đã được sử dụng");
        }

        if (bindingResult.hasErrors()) {
            form.setId(id);
            model.addAttribute("pageTitle", "Sửa người dùng");
            model.addAttribute("pageSubtitle", "Cập nhật tài khoản Ban quản lý");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("buildings", buildingService.listAll());
            return "admin/users/form";
        }

        try {
            userService.updateBqlUser(id, form);
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_UPDATE", "USER", form.getUsername(),
                        "Cập nhật tài khoản Ban quản lý: " + form.getUsername() + " (" + form.getRole() + ")", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_UPDATE", "USER", form.getUsername(),
                        "Lỗi cập nhật tài khoản Ban quản lý: " + ex.getMessage(), "FAILURE");
            }
            form.setId(id);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Sửa người dùng");
            model.addAttribute("pageSubtitle", "Cập nhật tài khoản Ban quản lý");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("buildings", buildingService.listAll());
            return "admin/users/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleStatus(id);
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_TOGGLE_STATUS", "USER", String.valueOf(id),
                        "Thay đổi trạng thái tài khoản Ban quản lý ID " + id, "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tài khoản thành công!");
        } catch (Exception ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_TOGGLE_STATUS", "USER", String.valueOf(id),
                        "Lỗi thay đổi trạng thái tài khoản: " + ex.getMessage(), "FAILURE");
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id, @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_RESET_PASSWORD", "USER", String.valueOf(id),
                        "Đặt lại mật khẩu tài khoản Ban quản lý ID " + id, "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công!");
        } catch (Exception ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("USER_RESET_PASSWORD", "USER", String.valueOf(id),
                        "Lỗi đặt lại mật khẩu tài khoản: " + ex.getMessage(), "FAILURE");
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
