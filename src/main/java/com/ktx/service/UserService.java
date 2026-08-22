package com.ktx.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.domain.User;
import com.ktx.domain.enums.Role;
import com.ktx.dto.BqlUserForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final BuildingRepository buildingRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, StaffRepository staffRepository,
            BuildingRepository buildingRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<BqlUserForm> listBqlUsers() {
        List<User> users = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.STAFF));
        List<Staff> staffList = staffRepository.findAllWithUserAndBuilding();

        Map<Long, Staff> staffMap = new HashMap<>();
        for (Staff s : staffList) {
            staffMap.put(s.getUser().getId(), s);
        }

        List<BqlUserForm> dtos = new ArrayList<>();
        for (User user : users) {
            BqlUserForm dto = new BqlUserForm();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setEnabled(user.getEnabled());

            if (user.getRole() == Role.STAFF) {
                Staff staff = staffMap.get(user.getId());
                if (staff != null) {
                    dto.setFullName(staff.getFullName());
                    dto.setPhone(staff.getPhone());
                    if (staff.getAssignedBuilding() != null) {
                        dto.setAssignedBuildingId(staff.getAssignedBuilding().getId());
                        dto.setAssignedBuildingCode(staff.getAssignedBuilding().getCode());
                    }
                }
            } else {
                dto.setFullName("Quản trị viên");
                dto.setPhone("—");
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public BqlUserForm getBqlUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            throw new BusinessException("Không phải tài khoản Ban quản lý");
        }

        BqlUserForm dto = new BqlUserForm();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());

        if (user.getRole() == Role.STAFF) {
            Staff staff = staffRepository.findByUserId(userId).orElse(null);
            if (staff != null) {
                dto.setFullName(staff.getFullName());
                dto.setPhone(staff.getPhone());
                if (staff.getAssignedBuilding() != null) {
                    dto.setAssignedBuildingId(staff.getAssignedBuilding().getId());
                    dto.setAssignedBuildingCode(staff.getAssignedBuilding().getCode());
                }
            }
        }
        return dto;
    }

    @Transactional
    public void createBqlUser(BqlUserForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new BusinessException("Tên đăng nhập đã được sử dụng");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new BusinessException("Email đã được sử dụng");
        }

        if (form.getRole() == Role.STAFF && form.getAssignedBuildingId() == null) {
            throw new BusinessException("Cán bộ quản lý tòa nhà (STAFF) bắt buộc phải chọn tòa nhà");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        if (form.getRole() == Role.STAFF) {
            Building building = buildingRepository.findById(form.getAssignedBuildingId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tòa nhà được chọn"));

            Staff staff = new Staff();
            staff.setUser(user);
            staff.setFullName(form.getFullName());
            staff.setPhone(form.getPhone());
            staff.setAssignedBuilding(building);
            staffRepository.save(staff);
        }
    }

    @Transactional
    public void updateBqlUser(Long userId, BqlUserForm form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        if (!user.getUsername().equals(form.getUsername()) && userRepository.existsByUsername(form.getUsername())) {
            throw new BusinessException("Tên đăng nhập đã được sử dụng");
        }
        if (!user.getEmail().equals(form.getEmail()) && userRepository.existsByEmail(form.getEmail())) {
            throw new BusinessException("Email đã được sử dụng");
        }

        if (form.getRole() == Role.STAFF && form.getAssignedBuildingId() == null) {
            throw new BusinessException("Cán bộ quản lý tòa nhà (STAFF) bắt buộc phải chọn tòa nhà");
        }

        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        Role oldRole = user.getRole();
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        if (form.getRole() == Role.STAFF) {
            Building building = buildingRepository.findById(form.getAssignedBuildingId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tòa nhà được chọn"));

            Staff staff = staffRepository.findByUserId(userId).orElse(new Staff());
            staff.setUser(user);
            staff.setFullName(form.getFullName());
            staff.setPhone(form.getPhone());
            staff.setAssignedBuilding(building);
            staffRepository.save(staff);
        } else if (oldRole == Role.STAFF) {
            // Delete Staff record if downgraded to ADMIN
            staffRepository.findByUserId(userId).ifPresent(staffRepository::delete);
        }
    }

    @Transactional
    public void toggleStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        user.setEnabled(!user.getEnabled());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new BusinessException("Mật khẩu phải từ 8 ký tự trở lên");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
