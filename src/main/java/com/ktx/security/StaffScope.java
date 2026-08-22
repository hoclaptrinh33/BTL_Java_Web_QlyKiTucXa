package com.ktx.security;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Room;
import com.ktx.domain.Staff;
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
        if (isAdmin(auth)) {
            return Optional.empty();
        }
        return Optional.of(requireStaffBuildingId(auth));
    }

    @Transactional(readOnly = true)
    public void assertBuilding(Authentication auth, long buildingId) {
        if (isAdmin(auth)) {
            return;
        }
        Long assigned = requireStaffBuildingId(auth);
        if (!assigned.equals(buildingId)) {
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

    private Long requireStaffBuildingId(Authentication auth) {
        requireAuthenticated(auth);
        if (!hasRole(auth, "ROLE_STAFF")) {
            throw new AccessDeniedException(DENIED_BUILDING);
        }
        Staff staff = staffRepository.findByUserUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException(DENIED_STAFF));
        return staff.getAssignedBuilding().getId();
    }

    private static void requireAuthenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }
    }

    private static boolean isAdmin(Authentication auth) {
        requireAuthenticated(auth);
        return hasRole(auth, "ROLE_ADMIN");
    }

    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
