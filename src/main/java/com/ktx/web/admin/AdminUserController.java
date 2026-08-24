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
import com.ktx.service.BuildingService;
import com.ktx.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final BuildingService buildingService;

    public AdminUserController(UserService userService, BuildingService buildingService) {
        this.userService = userService;
        this.buildingService = buildingService;
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
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Thêm người dùng");
            model.addAttribute("pageSubtitle", "Tạo tài khoản Ban quản lý mới");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("buildings", buildingService.listAll());
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BqlUserForm form = userService.getBqlUserById(id);
        model.addAttribute("pageTitle", "Sửa người dùng");
        model.addAttribute("pageSubtitle", "Cập nhật tài khoản Ban quản lý");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("userForm", form);
        model.addAttribute("buildings", buildingService.listAll());
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("userForm") BqlUserForm form,
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
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
            return "redirect:/admin/users";
        } catch (Exception ex) {
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
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tài khoản thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
