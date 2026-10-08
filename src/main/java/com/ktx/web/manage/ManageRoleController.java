package com.ktx.web.manage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.ktx.service.AuditLogService;
import com.ktx.service.RoleService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/manage/roles")
public class ManageRoleController {

    private final RoleService roleService;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public ManageRoleController(RoleService roleService,
                                @org.springframework.beans.factory.annotation.Autowired(required = false) AuditLogService auditLogService) {
        this.roleService = roleService;
        this.auditLogService = auditLogService;
    }

    public ManageRoleController(RoleService roleService) {
        this(roleService, null);
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Vai trò & Phân quyền");
        model.addAttribute("pageSubtitle", "Quản lý vai trò vận hành và thiết lập quyền truy cập");
        model.addAttribute("activeMenu", "roles");
        List<RoleDto> roles = roleService.listRoles();
        List<PermissionDto> allPermissions = roleService.getAllPermissions();
        List<PermissionDto> operationPermissions = roleService.getOperationPermissions();

        model.addAttribute("roles", roles);
        model.addAttribute("allPermissions", allPermissions);
        model.addAttribute("operationPermissions", operationPermissions);

        Map<String, List<PermissionDto>> permissionsByCategory = allPermissions.stream()
                .collect(Collectors.groupingBy(PermissionDto::getCategory, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("permissionsByCategory", permissionsByCategory);

        long totalAssignedUsers = roles.stream().mapToLong(RoleDto::getUserCount).sum();
        model.addAttribute("totalAssignedUsers", totalAssignedUsers);

        long systemLockedCount = roles.stream().filter(RoleDto::isSystemLocked).count();
        model.addAttribute("systemLockedCount", systemLockedCount);
        model.addAttribute("totalPermissionsCount", allPermissions.size());

        return "manage/roles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Thêm vai trò vận hành");
        model.addAttribute("pageSubtitle", "Tạo vai trò vận hành mới với quyền OPERATION");
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roleForm", new RoleDto());
        populateFormPermissions(model);
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
            populateFormPermissions(model);
            return "manage/roles/form";
        }

        try {
            roleService.createRole(form, auth);
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_CREATE", "ROLE", form.getCode(),
                        "Tạo vai trò vận hành mới: " + form.getName() + " (" + form.getCode() + ")", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Tạo vai trò '" + form.getCode() + "' thành công!");
            return "redirect:/manage/roles";
        } catch (BusinessException ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_CREATE", "ROLE", form.getCode(),
                        "Lỗi tạo vai trò '" + form.getCode() + "': " + ex.getMessage(), "FAILURE");
            }
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Thêm vai trò vận hành");
            model.addAttribute("pageSubtitle", "Tạo vai trò vận hành mới với quyền OPERATION");
            model.addAttribute("activeMenu", "roles");
            populateFormPermissions(model);
            return "manage/roles/form";
        }
    }

    @GetMapping("/{code}/edit")
    public String editForm(@PathVariable("code") String code, Model model) {
        RoleDto role = roleService.getRoleByCode(code);
        model.addAttribute("pageTitle", "Sửa vai trò: " + role.getName());
        model.addAttribute("pageSubtitle", role.isSystemLocked()
                ? "Vai trò mặc định hệ thống — chỉ có thể sửa mô tả"
                : "Chỉnh sửa vai trò vận hành");
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roleForm", role);
        populateFormPermissions(model);
        return "manage/roles/form";
    }

    @PostMapping("/{code}")
    public String update(@PathVariable("code") String code,
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
            populateFormPermissions(model);
            return "manage/roles/form";
        }

        try {
            roleService.updateRole(code, form, auth);
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_UPDATE", "ROLE", code,
                        "Cập nhật vai trò vận hành: " + form.getName() + " (" + code + ")", "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò '" + code + "' thành công!");
            return "redirect:/manage/roles";
        } catch (BusinessException ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_UPDATE", "ROLE", code,
                        "Lỗi cập nhật vai trò '" + code + "': " + ex.getMessage(), "FAILURE");
            }
            form.setCode(code);
            form.setSystemLocked(existing.isSystemLocked());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Sửa vai trò: " + existing.getName());
            model.addAttribute("pageSubtitle", existing.isSystemLocked()
                    ? "Vai trò mặc định hệ thống — chỉ có thể sửa mô tả"
                    : "Chỉnh sửa vai trò vận hành");
            model.addAttribute("activeMenu", "roles");
            populateFormPermissions(model);
            return "manage/roles/form";
        }
    }

    private void populateFormPermissions(Model model) {
        List<PermissionDto> opPerms = roleService.getOperationPermissions();
        model.addAttribute("operationPermissions", opPerms);
        Map<String, List<PermissionDto>> operationPermissionsByCategory = opPerms.stream()
                .collect(Collectors.groupingBy(PermissionDto::getCategory, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("operationPermissionsByCategory", operationPermissionsByCategory);
    }

    @PostMapping("/{code}/delete")
    public String delete(@PathVariable("code") String code, RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(code);
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_DELETE", "ROLE", code,
                        "Xóa vai trò vận hành: " + code, "SUCCESS");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vai trò '" + code + "' thành công!");
        } catch (BusinessException ex) {
            if (auditLogService != null) {
                auditLogService.logCurrent("ROLE_DELETE", "ROLE", code,
                        "Lỗi xóa vai trò '" + code + "': " + ex.getMessage(), "FAILURE");
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manage/roles";
    }
}
