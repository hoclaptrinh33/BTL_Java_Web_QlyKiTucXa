package com.ktx.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.ktx.common.exception.BusinessException;
import com.ktx.dto.PermissionDto;
import com.ktx.dto.RoleDto;
import com.ktx.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

    public static final Set<String> SYSTEM_PERMISSIONS = Set.of(
            "config.read",
            "config.write",
            "admin_account.manage"
    );

    public static final Set<String> OPERATION_PERMISSIONS = Set.of(
            "student.read", "student.write",
            "building.read", "building.write",
            "room.read", "room.write",
            "period.manage",
            "application.read",
            "allocation.manage",
            "contract.read", "contract.write",
            "invoice.read", "invoice.issue",
            "payment.record",
            "meter.read",
            "ticket.handle",
            "violation.write",
            "checkin.operate",
            "checkout.force",
            "report.read",
            "user.manage",
            "role.manage",
            "building.assign"
    );

    private final Map<String, RoleDto> roleStore = new ConcurrentHashMap<>();
    private final Map<Long, List<String>> userRoles = new ConcurrentHashMap<>();
    private final Map<Long, String> userScopes = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> userBuildings = new ConcurrentHashMap<>();

    public RoleServiceImpl() {
        initDefaultRoles();
    }

    private void initDefaultRoles() {
        RoleDto systemAdmin = new RoleDto(
                "SYSTEM_ADMIN",
                "Quản trị hệ thống",
                "Toàn quyền quản trị cấu hình hệ thống, tham số và tài khoản quản trị",
                true,
                "ALL",
                new HashSet<>(SYSTEM_PERMISSIONS)
        );

        RoleDto quanLy = new RoleDto(
                "QUAN_LY",
                "Quản lý KTX",
                "Toàn quyền vận hành toàn bộ ký túc xá (phòng, sinh viên, đơn, hợp đồng, hóa đơn, người dùng, vai trò)",
                true,
                "ALL",
                new HashSet<>(OPERATION_PERMISSIONS)
        );

        RoleDto canBo = new RoleDto(
                "CAN_BO",
                "Cán bộ tòa nhà",
                "Vận hành tác vụ theo tòa nhà được phân công (phòng, điện nước, checkin/out, sự cố, vi phạm)",
                true,
                "BUILDINGS",
                Set.of("room.read", "ticket.handle", "violation.write", "checkin.operate", "meter.read")
        );

        roleStore.put(systemAdmin.getCode(), systemAdmin);
        roleStore.put(quanLy.getCode(), quanLy);
        roleStore.put(canBo.getCode(), canBo);
    }

    @Override
    public List<RoleDto> listRoles() {
        List<RoleDto> list = new ArrayList<>(roleStore.values());
        for (RoleDto r : list) {
            int count = 0;
            for (List<String> roles : userRoles.values()) {
                if (roles.contains(r.getCode())) {
                    count++;
                }
            }
            r.setUserCount(count);
        }
        return list;
    }

    @Override
    public Optional<RoleDto> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        RoleDto role = roleStore.get(code.toUpperCase());
        if (role != null) {
            int count = 0;
            for (List<String> roles : userRoles.values()) {
                if (roles.contains(role.getCode())) {
                    count++;
                }
            }
            role.setUserCount(count);
        }
        return Optional.ofNullable(role);
    }

    @Override
    public RoleDto getRoleByCode(String code) {
        return findByCode(code).orElseThrow(() -> new BusinessException("Không tìm thấy vai trò '" + code + "'"));
    }

    @Override
    public List<PermissionDto> getOperationPermissions() {
        List<PermissionDto> list = new ArrayList<>();
        list.add(new PermissionDto("student.read", "Xem danh sách và hồ sơ sinh viên", "Sinh viên", false));
        list.add(new PermissionDto("student.write", "Thêm, cập nhật hồ sơ sinh viên", "Sinh viên", false));
        list.add(new PermissionDto("building.read", "Xem danh sách và sơ đồ tòa nhà", "Tòa nhà", false));
        list.add(new PermissionDto("building.write", "Thêm, sửa thông tin tòa nhà", "Tòa nhà", false));
        list.add(new PermissionDto("room.read", "Xem danh sách phòng, giường, tài sản", "Phòng ở", false));
        list.add(new PermissionDto("room.write", "Thêm, sửa phòng, giường và tài sản", "Phòng ở", false));
        list.add(new PermissionDto("period.manage", "Quản lý đợt đăng ký chỗ ở", "Đợt & Đơn", false));
        list.add(new PermissionDto("application.read", "Xem và duyệt đơn đăng ký nguyện vọng", "Đợt & Đơn", false));
        list.add(new PermissionDto("allocation.manage", "Chạy phân bổ, xem preview và xếp phòng", "Phân bổ", false));
        list.add(new PermissionDto("contract.read", "Xem danh sách và chi tiết hợp đồng lưu trú", "Hợp đồng", false));
        list.add(new PermissionDto("contract.write", "Lập, gia hạn, chuyển phòng và chấm dứt hợp đồng", "Hợp đồng", false));
        list.add(new PermissionDto("invoice.read", "Xem danh sách và chi tiết hóa đơn", "Hóa đơn & Tiền", false));
        list.add(new PermissionDto("invoice.issue", "Phát hành hóa đơn tiền phòng và điện nước", "Hóa đơn & Tiền", false));
        list.add(new PermissionDto("payment.record", "Ghi nhận thanh toán và gạch nợ hóa đơn", "Hóa đơn & Tiền", false));
        list.add(new PermissionDto("meter.read", "Ghi chỉ số điện nước theo phòng / tòa", "Điện nước", false));
        list.add(new PermissionDto("ticket.handle", "Tiếp nhận, xử lý và cập nhật báo hỏng sự cố", "Vận hành", false));
        list.add(new PermissionDto("violation.write", "Lập biên bản và ghi nhận vi phạm quy chế", "Vận hành", false));
        list.add(new PermissionDto("checkin.operate", "Thực hiện thủ tục check-in nhận phòng và check-out", "Check-in/out", false));
        list.add(new PermissionDto("checkout.force", "Buộc check-out khi sinh viên còn nợ quá hạn (Quản lý)", "Check-in/out", false));
        list.add(new PermissionDto("report.read", "Xem báo cáo thống kê tỷ lệ lấp đầy và công nợ", "Báo cáo", false));
        list.add(new PermissionDto("user.manage", "Quản lý tài khoản cán bộ và nhân viên nội bộ", "Phân quyền", false));
        list.add(new PermissionDto("role.manage", "Quản lý tạo và phân quyền vai trò vận hành", "Phân quyền", false));
        list.add(new PermissionDto("building.assign", "Phân công cán bộ quản lý tòa nhà", "Phân công", false));
        return list;
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        List<PermissionDto> list = new ArrayList<>();
        list.add(new PermissionDto("config.read", "Xem cấu hình hệ thống", "Hệ thống", true));
        list.add(new PermissionDto("config.write", "Chỉnh sửa cấu hình hệ thống", "Hệ thống", true));
        list.add(new PermissionDto("admin_account.manage", "Quản lý tài khoản Admin hệ thống", "Hệ thống", true));
        list.addAll(getOperationPermissions());
        return list;
    }

    @Override
    public RoleDto createRole(RoleDto dto, Authentication auth) {
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new BusinessException("Mã vai trò không được để trống");
        }
        String code = dto.getCode().trim().toUpperCase();
        if (roleStore.containsKey(code)) {
            throw new BusinessException("Mã vai trò '" + code + "' đã tồn tại");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("Tên vai trò không được để trống");
        }

        validatePermissions(dto.getPermissions(), auth);

        RoleDto role = new RoleDto();
        role.setCode(code);
        role.setName(dto.getName().trim());
        role.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        role.setSystemLocked(false);
        role.setScope("BUILDINGS".equalsIgnoreCase(dto.getScope()) ? "BUILDINGS" : "ALL");
        role.setPermissions(dto.getPermissions());

        roleStore.put(code, role);
        return role;
    }

    @Override
    public RoleDto updateRole(String code, RoleDto dto, Authentication auth) {
        RoleDto existing = getRoleByCode(code);
        if (existing.isSystemLocked()) {
            existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
            return existing;
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException("Tên vai trò không được để trống");
        }

        validatePermissions(dto.getPermissions(), auth);

        existing.setName(dto.getName().trim());
        existing.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        existing.setScope("BUILDINGS".equalsIgnoreCase(dto.getScope()) ? "BUILDINGS" : "ALL");
        existing.setPermissions(dto.getPermissions());
        return existing;
    }

    @Override
    public void deleteRole(String code) {
        RoleDto existing = getRoleByCode(code);
        if (existing.isSystemLocked()) {
            throw new BusinessException("Không thể xóa vai trò mặc định của hệ thống (" + existing.getCode() + ")");
        }
        for (List<String> assigned : userRoles.values()) {
            if (assigned.contains(existing.getCode())) {
                throw new BusinessException("Không thể xóa vai trò đang được gán cho người dùng");
            }
        }
        roleStore.remove(existing.getCode());
    }

    private void validatePermissions(Set<String> requestedPermissions, Authentication auth) {
        if (requestedPermissions == null || requestedPermissions.isEmpty()) {
            return;
        }
        for (String perm : requestedPermissions) {
            if (SYSTEM_PERMISSIONS.contains(perm)) {
                throw new BusinessException("Không thể gắn quyền SYSTEM ('" + perm + "') cho vai trò vận hành");
            }
            if (!OPERATION_PERMISSIONS.contains(perm)) {
                throw new BusinessException("Mã quyền '" + perm + "' không thuộc hợp đồng quyền OPERATION");
            }
        }

        if (auth != null && auth.getAuthorities() != null) {
            Set<String> authStrings = new HashSet<>();
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga != null && ga.getAuthority() != null) {
                    authStrings.add(ga.getAuthority());
                }
            }
            boolean isSuper = authStrings.contains("ROLE_ADMIN")
                    || authStrings.contains("ROLE_QUAN_LY")
                    || authStrings.contains("QUAN_LY");

            if (!isSuper) {
                for (String perm : requestedPermissions) {
                    if (!authStrings.contains(perm)) {
                        throw new BusinessException("Bạn không thể gán quyền '" + perm + "' vì tài khoản của bạn chưa có quyền này");
                    }
                }
            }
        }
    }

    @Override
    public List<String> getRolesForUser(Long userId) {
        return userRoles.getOrDefault(userId, Collections.emptyList());
    }

    @Override
    public void setRolesForUser(Long userId, List<String> roleCodes, String scope, List<Long> buildingIds) {
        if (userId == null) {
            return;
        }
        List<String> validCodes = new ArrayList<>();
        if (roleCodes != null) {
            for (String c : roleCodes) {
                if (c != null && !c.trim().isEmpty()) {
                    validCodes.add(c.trim().toUpperCase());
                }
            }
        }
        userRoles.put(userId, validCodes);
        userScopes.put(userId, "BUILDINGS".equalsIgnoreCase(scope) ? "BUILDINGS" : "ALL");
        if (buildingIds != null) {
            userBuildings.put(userId, new ArrayList<>(buildingIds));
        } else {
            userBuildings.put(userId, new ArrayList<>());
        }
    }

    @Override
    public String getScopeForUser(Long userId) {
        return userScopes.getOrDefault(userId, "ALL");
    }

    @Override
    public List<Long> getAssignedBuildingIdsForUser(Long userId) {
        return userBuildings.getOrDefault(userId, Collections.emptyList());
    }
}
