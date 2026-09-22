package com.ktx.web.manage;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.dto.PermissionDto;
import com.ktx.dto.RoleDto;
import com.ktx.service.RoleService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/manage/roles")
public class ManageRoleController {

    private final RoleService roleService;

    public ManageRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Vai trò & Phân quyền");
        model.addAttribute("pageSubtitle", "Quản lý vai trò vận hành và thiết lập quyền truy cập");
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roles", roleService.listRoles());
        return "manage/roles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Thêm vai trò vận hành");
        model.addAttribute("pageSubtitle", "Tạo vai trò vận hành mới với quyền OPERATION");
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roleForm", new RoleDto());
        model.addAttribute("operationPermissions", roleService.getOperationPermissions());
        return "manage/roles/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roleForm") RoleDto form,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm vai trò vận hành");
            model.addAttribute("pageSubtitle", "Tạo vai trò vận hành mới với quyền OPERATION");
            model.addAttribute("activeMenu", "roles");
            model.addAttribute("operationPermissions", roleService.getOperationPermissions());
            return "manage/roles/form";
        }

        try {
            roleService.createRole(form, auth);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò '" + form.getCode() + "' thành công!");
            return "redirect:/manage/roles";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Thêm vai trò vận hành");
            model.addAttribute("pageSubtitle", "Tạo vai trò vận hành mới với quyền OPERATION");
            model.addAttribute("activeMenu", "roles");
            model.addAttribute("operationPermissions", roleService.getOperationPermissions());
            return "manage/roles/form";
        }
    }

    @GetMapping("/{code}/edit")
    public String editForm(@PathVariable String code, Model model) {
        RoleDto role = roleService.getRoleByCode(code);
        model.addAttribute("pageTitle", "Sửa vai trò: " + role.getName());
        model.addAttribute("pageSubtitle", role.isSystemLocked()
                ? "Vai trò mặc định hệ thống — chỉ có thể sửa mô tả"
                : "Chỉnh sửa vai trò vận hành");
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roleForm", role);
        model.addAttribute("operationPermissions", roleService.getOperationPermissions());
        return "manage/roles/form";
    }

    @PostMapping("/{code}")
    public String update(@PathVariable String code,
                         @Valid @ModelAttribute("roleForm") RoleDto form,
                         BindingResult bindingResult,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        RoleDto existing = roleService.getRoleByCode(code);
        if (!existing.isSystemLocked() && bindingResult.hasErrors()) {
            form.setCode(code);
            form.setSystemLocked(false);
            model.addAttribute("pageTitle", "Sửa vai trò: " + form.getName());
            model.addAttribute("pageSubtitle", "Chỉnh sửa vai trò vận hành");
            model.addAttribute("activeMenu", "roles");
            model.addAttribute("operationPermissions", roleService.getOperationPermissions());
            return "manage/roles/form";
        }

        try {
            roleService.updateRole(code, form, auth);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò '" + code + "' thành công!");
            return "redirect:/manage/roles";
        } catch (BusinessException ex) {
            form.setCode(code);
            form.setSystemLocked(existing.isSystemLocked());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Sửa vai trò: " + existing.getName());
            model.addAttribute("pageSubtitle", existing.isSystemLocked()
                    ? "Vai trò mặc định hệ thống — chỉ có thể sửa mô tả"
                    : "Chỉnh sửa vai trò vận hành");
            model.addAttribute("activeMenu", "roles");
            model.addAttribute("operationPermissions", roleService.getOperationPermissions());
            return "manage/roles/form";
        }
    }

    @PostMapping("/{code}/delete")
    public String delete(@PathVariable String code, RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(code);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vai trò '" + code + "' thành công!");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manage/roles";
    }
}
