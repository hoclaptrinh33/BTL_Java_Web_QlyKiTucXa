package com.ktx.web.manage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.BqlUserForm;
import com.ktx.dto.InternalUserForm;
import com.ktx.dto.RoleDto;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.BuildingService;
import com.ktx.service.RoleService;
import com.ktx.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/manage/users")
public class ManageUserController {

    private final UserService userService;
    private final RoleService roleService;
    private final BuildingService buildingService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final PasswordEncoder passwordEncoder;

    public ManageUserController(UserService userService,
                                RoleService roleService,
                                BuildingService buildingService,
                                UserRepository userRepository,
                                StaffRepository staffRepository,
                                StudentRepository studentRepository,
                                BuildingRepository buildingRepository,
                                PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.buildingService = buildingService;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Người dùng nội bộ");
        model.addAttribute("pageSubtitle", "Quản lý nhân sự, gán vai trò và phân công phạm vi tòa");
        model.addAttribute("activeMenu", "users");

        List<BqlUserForm> rawUsers = userService.listBqlUsers();
        // Enrich with multi-roles and assigned buildings info
        for (BqlUserForm u : rawUsers) {
            List<String> assignedRoles = roleService.getRolesForUser(u.getId());
            if (assignedRoles.isEmpty()) {
                if (u.getRole() == Role.ADMIN) {
                    assignedRoles = List.of("QUAN_LY");
                } else if (u.getRole() == Role.STAFF) {
                    assignedRoles = List.of("CAN_BO");
                }
            }
            // Temporarily store in assignedBuildingCode display if empty
            if (u.getAssignedBuildingCode() == null && "ALL".equalsIgnoreCase(roleService.getScopeForUser(u.getId()))) {
                u.setAssignedBuildingCode("Toàn KTX (ALL)");
            }
        }

        model.addAttribute("users", rawUsers);
        model.addAttribute("roleService", roleService);
        return "manage/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Thêm người dùng nội bộ");
        model.addAttribute("pageSubtitle", "Tạo tài khoản cán bộ và gán vai trò vận hành");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("userForm", new InternalUserForm());
        model.addAttribute("availableRoles", getAssignableInternalRoles());
        model.addAttribute("buildings", buildingService.listAll());
        return "manage/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") InternalUserForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validateInternalUserForm(form, bindingResult, true, null);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Thêm người dùng nội bộ");
            model.addAttribute("pageSubtitle", "Tạo tài khoản cán bộ và gán vai trò vận hành");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("availableRoles", getAssignableInternalRoles());
            model.addAttribute("buildings", buildingService.listAll());
            return "manage/users/form";
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            User user = new User();
            user.setUsername(form.getUsername().trim());
            user.setEmail(form.getEmail().trim());
            user.setPasswordHash(passwordEncoder.encode(form.getPassword().trim()));

            boolean isAllScope = "ALL".equalsIgnoreCase(form.getScope())
                    || form.getRoles().contains("QUAN_LY");

            user.setRole(isAllScope ? Role.ADMIN : Role.STAFF);
            user.setEnabled(form.isEnabled());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user = userRepository.save(user);

            if (!form.getAssignedBuildingIds().isEmpty()) {
                Long primaryBuildingId = form.getAssignedBuildingIds().get(0);
                Building building = buildingRepository.findById(primaryBuildingId)
                        .orElse(null);

                Staff staff = new Staff();
                staff.setUser(user);
                staff.setFullName(form.getFullName() != null ? form.getFullName().trim() : user.getUsername());
                staff.setPhone(form.getPhone());
                staff.setAssignedBuilding(building);
                staffRepository.save(staff);
            } else if (!isAllScope) {
                // Building scope requires a building
                Building firstBuilding = buildingRepository.findAll().stream().findFirst().orElse(null);
                Staff staff = new Staff();
                staff.setUser(user);
                staff.setFullName(form.getFullName() != null ? form.getFullName().trim() : user.getUsername());
                staff.setPhone(form.getPhone());
                staff.setAssignedBuilding(firstBuilding);
                staffRepository.save(staff);
            }

            roleService.setRolesForUser(user.getId(), form.getRoles(), form.getScope(), form.getAssignedBuildingIds());

            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản người dùng nội bộ thành công!");
            return "redirect:/manage/users";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Thêm người dùng nội bộ");
            model.addAttribute("pageSubtitle", "Tạo tài khoản cán bộ và gán vai trò vận hành");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("availableRoles", getAssignableInternalRoles());
            model.addAttribute("buildings", buildingService.listAll());
            return "manage/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        if (user.getRole() == Role.STUDENT || studentRepository.findByUserId(id).isPresent()) {
            throw new BusinessException("Tài khoản sinh viên không nhận vai nội bộ");
        }

        InternalUserForm form = new InternalUserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setEnabled(Boolean.TRUE.equals(user.getEnabled()));

        Staff staff = staffRepository.findByUserId(id).orElse(null);
        if (staff != null) {
            form.setFullName(staff.getFullName());
            form.setPhone(staff.getPhone());
            if (staff.getAssignedBuilding() != null) {
                form.setAssignedBuildingIds(List.of(staff.getAssignedBuilding().getId()));
            }
        }

        List<String> assignedRoles = roleService.getRolesForUser(id);
        if (assignedRoles.isEmpty()) {
            if (user.getRole() == Role.ADMIN) {
                assignedRoles = List.of("QUAN_LY");
            } else if (user.getRole() == Role.STAFF) {
                assignedRoles = List.of("CAN_BO");
            }
        }
        form.setRoles(assignedRoles);
        form.setScope(roleService.getScopeForUser(id));

        List<Long> extraBuildings = roleService.getAssignedBuildingIdsForUser(id);
        if (!extraBuildings.isEmpty()) {
            form.setAssignedBuildingIds(extraBuildings);
        }

        model.addAttribute("pageTitle", "Sửa người dùng nội bộ: " + form.getUsername());
        model.addAttribute("pageSubtitle", "Cập nhật vai trò và phân công tòa nhà");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("userForm", form);
        model.addAttribute("availableRoles", getAssignableInternalRoles());
        model.addAttribute("buildings", buildingService.listAll());
        return "manage/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") InternalUserForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        validateInternalUserForm(form, bindingResult, false, id);

        if (bindingResult.hasErrors()) {
            form.setId(id);
            model.addAttribute("pageTitle", "Sửa người dùng nội bộ: " + form.getUsername());
            model.addAttribute("pageSubtitle", "Cập nhật vai trò và phân công tòa nhà");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("availableRoles", getAssignableInternalRoles());
            model.addAttribute("buildings", buildingService.listAll());
            return "manage/users/form";
        }

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

            if (user.getRole() == Role.STUDENT || studentRepository.findByUserId(id).isPresent()) {
                throw new BusinessException("Tài khoản sinh viên không nhận vai nội bộ");
            }

            user.setUsername(form.getUsername().trim());
            user.setEmail(form.getEmail().trim());
            user.setEnabled(form.isEnabled());

            boolean isAllScope = "ALL".equalsIgnoreCase(form.getScope())
                    || form.getRoles().contains("QUAN_LY");
            user.setRole(isAllScope ? Role.ADMIN : Role.STAFF);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            if (!form.getAssignedBuildingIds().isEmpty()) {
                Building building = buildingRepository.findById(form.getAssignedBuildingIds().get(0)).orElse(null);
                Staff staff = staffRepository.findByUserId(id).orElse(new Staff());
                staff.setUser(user);
                staff.setFullName(form.getFullName() != null ? form.getFullName().trim() : user.getUsername());
                staff.setPhone(form.getPhone());
                staff.setAssignedBuilding(building);
                staffRepository.save(staff);
            } else if (staffRepository.findByUserId(id).isPresent()) {
                Staff staff = staffRepository.findByUserId(id).get();
                staff.setFullName(form.getFullName() != null ? form.getFullName().trim() : user.getUsername());
                staff.setPhone(form.getPhone());
                staffRepository.save(staff);
            }

            roleService.setRolesForUser(id, form.getRoles(), form.getScope(), form.getAssignedBuildingIds());

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng nội bộ thành công!");
            return "redirect:/manage/users";
        } catch (Exception ex) {
            form.setId(id);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Sửa người dùng nội bộ");
            model.addAttribute("pageSubtitle", "Cập nhật vai trò và phân công tòa nhà");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("availableRoles", getAssignableInternalRoles());
            model.addAttribute("buildings", buildingService.listAll());
            return "manage/users/form";
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
        return "redirect:/manage/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manage/users";
    }

    private List<RoleDto> getAssignableInternalRoles() {
        return roleService.listRoles().stream()
                .filter(r -> !"SYSTEM_ADMIN".equalsIgnoreCase(r.getCode()))
                .collect(Collectors.toList());
    }

    private void validateInternalUserForm(InternalUserForm form, BindingResult bindingResult, boolean isNew, Long existingId) {
        if (isNew && (form.getPassword() == null || form.getPassword().trim().length() < 8)) {
            bindingResult.rejectValue("password", "size", "Mật khẩu bắt buộc từ 8 ký tự trở lên");
        }

        // Rule: "Không gán SYSTEM_ADMIN từ màn này"
        if (form.getRoles() != null) {
            for (String r : form.getRoles()) {
                if ("SYSTEM_ADMIN".equalsIgnoreCase(r)) {
                    bindingResult.rejectValue("roles", "forbidden",
                            "Không thể gán vai trò Quản trị hệ thống (SYSTEM_ADMIN) từ màn này. Vui lòng tạo tài khoản quản trị tại Cổng Admin.");
                }
            }
        }

        if (form.getRoles() == null || form.getRoles().isEmpty()) {
            bindingResult.rejectValue("roles", "required", "Cần chọn ít nhất một vai trò nội bộ");
        }

        // Rule: "Tài khoản sinh viên không nhận vai nội bộ"
        if (isNew) {
            if (studentRepository.existsByStudentCode(form.getUsername())) {
                bindingResult.rejectValue("username", "student_conflict", "Tài khoản sinh viên không nhận vai nội bộ");
            }
            if (userRepository.existsByUsername(form.getUsername())) {
                bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã được sử dụng");
            }
            if (userRepository.existsByEmail(form.getEmail())) {
                bindingResult.rejectValue("email", "duplicate", "Email đã được sử dụng");
            }
        } else {
            if (studentRepository.existsByStudentCode(form.getUsername())) {
                bindingResult.rejectValue("username", "student_conflict", "Tài khoản sinh viên không nhận vai nội bộ");
            }
            User current = userRepository.findById(existingId).orElse(null);
            if (current != null) {
                if (current.getRole() == Role.STUDENT || studentRepository.findByUserId(existingId).isPresent()) {
                    bindingResult.rejectValue("username", "student_conflict", "Tài khoản sinh viên không nhận vai nội bộ");
                }
                if (!current.getUsername().equalsIgnoreCase(form.getUsername()) && userRepository.existsByUsername(form.getUsername())) {
                    bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã được sử dụng");
                }
                if (!current.getEmail().equalsIgnoreCase(form.getEmail()) && userRepository.existsByEmail(form.getEmail())) {
                    bindingResult.rejectValue("email", "duplicate", "Email đã được sử dụng");
                }
            }
        }

        if ("BUILDINGS".equalsIgnoreCase(form.getScope()) && (form.getAssignedBuildingIds() == null || form.getAssignedBuildingIds().isEmpty())) {
            bindingResult.rejectValue("assignedBuildingIds", "required", "Phạm vi theo tòa (BUILDINGS) bắt buộc phải chọn ít nhất một tòa nhà");
        }
    }
}
