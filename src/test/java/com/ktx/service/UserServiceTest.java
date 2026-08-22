package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.BqlUserForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private BuildingRepository buildingRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, staffRepository, buildingRepository, ENCODER);
    }

    @Test
    void createBqlUser_adminRole_savesUserOnly() {
        BqlUserForm form = new BqlUserForm();
        form.setUsername("adminB");
        form.setEmail("adminb@example.com");
        form.setPassword("password123");
        form.setRole(Role.ADMIN);
        form.setEnabled(true);

        when(userRepository.existsByUsername("adminB")).thenReturn(false);
        when(userRepository.existsByEmail("adminb@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createBqlUser(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("adminB", savedUser.getUsername());
        assertEquals("adminb@example.com", savedUser.getEmail());
        assertEquals(Role.ADMIN, savedUser.getRole());
        assertTrue(ENCODER.matches("password123", savedUser.getPasswordHash()));
        verify(staffRepository, never()).save(any());
    }

    @Test
    void createBqlUser_staffRole_savesUserAndStaff() {
        BqlUserForm form = new BqlUserForm();
        form.setUsername("staffB");
        form.setEmail("staffb@example.com");
        form.setPassword("password123");
        form.setRole(Role.STAFF);
        form.setFullName("Cán bộ B");
        form.setPhone("0987654321");
        form.setAssignedBuildingId(1L);
        form.setEnabled(true);

        Building building = new Building();
        building.setId(1L);

        when(userRepository.existsByUsername("staffB")).thenReturn(false);
        when(userRepository.existsByEmail("staffb@example.com")).thenReturn(false);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        userService.createBqlUser(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("staffB", savedUser.getUsername());

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository).save(staffCaptor.capture());
        Staff savedStaff = staffCaptor.getValue();

        assertEquals("Cán bộ B", savedStaff.getFullName());
        assertEquals("0987654321", savedStaff.getPhone());
        assertEquals(10L, savedStaff.getUser().getId());
        assertEquals(1L, savedStaff.getAssignedBuilding().getId());
    }

    @Test
    void createBqlUser_staffRoleMissingBuilding_throwsException() {
        BqlUserForm form = new BqlUserForm();
        form.setUsername("staffB");
        form.setEmail("staffb@example.com");
        form.setPassword("password123");
        form.setRole(Role.STAFF);
        form.setAssignedBuildingId(null); // Missing building

        when(userRepository.existsByUsername("staffB")).thenReturn(false);
        when(userRepository.existsByEmail("staffb@example.com")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.createBqlUser(form));
        assertTrue(ex.getMessage().contains("bắt buộc phải chọn tòa nhà"));
        verify(userRepository, never()).save(any());
        verify(staffRepository, never()).save(any());
    }

    @Test
    void updateBqlUser_staffToAdmin_deletesStaffRecord() {
        User user = new User();
        user.setId(5L);
        user.setUsername("user5");
        user.setEmail("user5@example.com");
        user.setRole(Role.STAFF);

        BqlUserForm form = new BqlUserForm();
        form.setUsername("user5");
        form.setEmail("user5@example.com");
        form.setRole(Role.ADMIN); // Change to ADMIN

        Staff staff = new Staff();
        staff.setId(10L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(staffRepository.findByUserId(5L)).thenReturn(Optional.of(staff));

        userService.updateBqlUser(5L, form);

        verify(userRepository).save(user);
        assertEquals(Role.ADMIN, user.getRole());
        verify(staffRepository).delete(staff);
    }
}
