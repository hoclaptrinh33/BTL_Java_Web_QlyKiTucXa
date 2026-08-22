package com.ktx.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.Staff;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.repository.StaffRepository;

@ExtendWith(MockitoExtension.class)
class StaffScopeTest {

    @Mock
    private StaffRepository staffRepository;

    private StaffScope staffScope;

    @BeforeEach
    void setUp() {
        staffScope = new StaffScope(staffRepository);
    }

    @Test
    void adminBuildingIdIsEmpty_noFilter() {
        Authentication admin = auth("admin", "ROLE_ADMIN");
        assertTrue(staffScope.buildingId(admin).isEmpty());
        staffScope.assertBuilding(admin, 99L);
        staffScope.assertRoom(admin, room(99L));
    }

    @Test
    void staffCanAssertOwnBuilding() {
        when(staffRepository.findByUserUsername("staffA")).thenReturn(Optional.of(staff("staffA", 1L)));
        Authentication staff = auth("staffA", "ROLE_STAFF");

        assertEquals(1L, staffScope.buildingId(staff).orElseThrow());
        staffScope.assertBuilding(staff, 1L);
        staffScope.assertRoom(staff, room(1L));
    }

    @Test
    void staffCannotAssertOtherBuilding() {
        when(staffRepository.findByUserUsername("staffA")).thenReturn(Optional.of(staff("staffA", 1L)));
        Authentication staff = auth("staffA", "ROLE_STAFF");

        AccessDeniedException buildingEx = assertThrows(AccessDeniedException.class,
                () -> staffScope.assertBuilding(staff, 2L));
        assertEquals(StaffScope.DENIED_BUILDING, buildingEx.getMessage());

        AccessDeniedException roomEx = assertThrows(AccessDeniedException.class,
                () -> staffScope.assertRoom(staff, room(2L)));
        assertEquals(StaffScope.DENIED_BUILDING, roomEx.getMessage());
    }

    @Test
    void studentIsDenied() {
        Authentication student = auth("sv", "ROLE_STUDENT");
        assertThrows(AccessDeniedException.class, () -> staffScope.buildingId(student));
        assertThrows(AccessDeniedException.class, () -> staffScope.assertBuilding(student, 1L));
    }

    @Test
    void staffWithoutRowIsDenied() {
        when(staffRepository.findByUserUsername("staffA")).thenReturn(Optional.empty());
        Authentication staff = auth("staffA", "ROLE_STAFF");
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> staffScope.buildingId(staff));
        assertEquals(StaffScope.DENIED_STAFF, ex.getMessage());
    }

    private static Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }

    private static Staff staff(String username, Long buildingId) {
        User user = new User();
        user.setUsername(username);
        user.setRole(Role.STAFF);
        Building building = new Building();
        building.setId(buildingId);
        building.setCode("A");
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setAssignedBuilding(building);
        return staff;
    }

    private static Room room(Long buildingId) {
        Building building = new Building();
        building.setId(buildingId);
        Room room = new Room();
        room.setBuilding(building);
        return room;
    }
}
