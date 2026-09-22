package com.ktx.security;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Building;
import com.ktx.domain.Role;
import com.ktx.domain.Room;
import com.ktx.domain.Staff;
import com.ktx.domain.enums.ScopeMode;
import com.ktx.repository.StaffRepository;

@Service
public class StaffScope {

    public static final String DENIED_BUILDING = "Không có quyền thao tác tòa này";
    public static final String DENIED_STAFF = "Tài khoản cán bộ chưa được gán tòa";

    private final StaffRepository staffRepository;

    public StaffScope(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Long> buildingId(Authentication auth) {
        if (isScopeAll(auth)) {
            return Optional.empty();
        }
        requireAuthenticated(auth);
        Staff staff = staffRepository.findByUserUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException(DENIED_STAFF));

        // Giữ staff.assigned_building_id đồng bộ với tòa đầu tiên để code cũ chưa gãy
        if (staff.getAssignedBuilding() != null && staff.getAssignedBuilding().getId() != null) {
            return Optional.of(staff.getAssignedBuilding().getId());
        }
        Set<Long> ids = getAssignedBuildingIds(auth);
        return Optional.of(ids.iterator().next());
    }

    @Transactional(readOnly = true)
    public void assertBuilding(Authentication auth, long buildingId) {
        if (isScopeAll(auth)) {
            return;
        }
        Set<Long> allowedBuildingIds = getAssignedBuildingIds(auth);
        if (!allowedBuildingIds.contains(buildingId)) {
            throw new AccessDeniedException(DENIED_BUILDING);
        }
    }

    @Transactional(readOnly = true)
    public void assertRoom(Authentication auth, Room room) {
        if (room == null || room.getBuilding() == null || room.getBuilding().getId() == null) {
            throw new AccessDeniedException(DENIED_BUILDING);
        }
        assertBuilding(auth, room.getBuilding().getId());
    }

    public Set<Long> getAssignedBuildingIds(Authentication auth) {
        requireAuthenticated(auth);
        if (!hasRole(auth, "ROLE_STAFF") && !hasAuthority(auth, "room.read")) {
            throw new AccessDeniedException(DENIED_BUILDING);
        }
        Staff staff = staffRepository.findByUserUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException(DENIED_STAFF));

        Set<Long> buildingIds = new HashSet<>();
        if (staff.getUser() != null && staff.getUser().getAssignedBuildings() != null) {
            for (Building b : staff.getUser().getAssignedBuildings()) {
                if (b != null && b.getId() != null) {
                    buildingIds.add(b.getId());
                }
            }
        }
        if (staff.getAssignedBuilding() != null && staff.getAssignedBuilding().getId() != null) {
            buildingIds.add(staff.getAssignedBuilding().getId());
        }
        if (buildingIds.isEmpty()) {
            throw new AccessDeniedException(DENIED_STAFF);
        }
        return buildingIds;
    }

    private boolean isScopeAll(Authentication auth) {
        requireAuthenticated(auth);
        if (hasRole(auth, "ROLE_ADMIN")) {
            return true;
        }
        if (hasAuthority(auth, "config.read") || hasAuthority(auth, "config.write")
                || hasAuthority(auth, "admin_account.manage")) {
            return true;
        }
        Staff staff = staffRepository.findByUserUsername(auth.getName()).orElse(null);
        if (staff != null && staff.getUser() != null && staff.getUser().getRoles() != null) {
            for (Role r : staff.getUser().getRoles()) {
                if (r != null && r.getScopeMode() == ScopeMode.ALL) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void requireAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }
    }

    private static boolean hasRole(Authentication auth, String role) {
        return hasAuthority(auth, role);
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a != null && authority.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
