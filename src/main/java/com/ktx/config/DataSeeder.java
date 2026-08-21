package com.ktx.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.Role;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.repository.UserRepository;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      BuildingRepository buildingRepository,
                      StaffRepository staffRepository,
                      StudentRepository studentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only seed if database is clean (e.g. no 'admin' user exists)
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        // 1. Seed Admin
        User adminUser = createUser("admin", "admin@example.com", "Admin@123", Role.ADMIN);
        userRepository.save(adminUser);

        // 2. Seed Buildings
        Building buildingA = new Building();
        buildingA.setCode("A");
        buildingA.setName("Tòa A");
        buildingA.setGenderPolicy(BuildingGenderPolicy.MALE);
        buildingA.setActive(true);
        buildingA = buildingRepository.save(buildingA);

        Building buildingB = new Building();
        buildingB.setCode("B");
        buildingB.setName("Tòa B");
        buildingB.setGenderPolicy(BuildingGenderPolicy.FEMALE);
        buildingB.setActive(true);
        buildingB = buildingRepository.save(buildingB);

        // 3. Seed Staff (assigned to building A)
        User staffUser = createUser("staffA", "staffa@example.com", "Admin@123", Role.STAFF);
        staffUser = userRepository.save(staffUser);

        Staff staff = new Staff();
        staff.setUser(staffUser);
        staff.setFullName("Cán bộ A");
        staff.setPhone("0912345678");
        staff.setAssignedBuilding(buildingA);
        staffRepository.save(staff);

        // 4. Seed Student D22CQCN001 (Nam, CNTT, D22CQCN01, POLICY)
        User student1User = createUser("D22CQCN001", "d22cqcn001@example.com", "Admin@123", Role.STUDENT);
        student1User = userRepository.save(student1User);

        Student student1 = new Student();
        student1.setUser(student1User);
        student1.setStudentCode("D22CQCN001");
        student1.setFullName("Nguyễn Văn Nam");
        student1.setGender(Gender.MALE);
        student1.setClassCode("D22CQCN01");
        student1.setFacultyCode("CNTT");
        student1.setPriorityCategory(PriorityCategory.POLICY);
        student1.setPreviousStayGood(true);
        student1.setConductScore(100);
        student1.setBlockedFromHousing(false);
        studentRepository.save(student1);

        // 5. Seed Student D22CQCN002 (Nam, CNTT, D22CQCN01, NONE)
        User student2User = createUser("D22CQCN002", "d22cqcn002@example.com", "Admin@123", Role.STUDENT);
        student2User = userRepository.save(student2User);

        Student student2 = new Student();
        student2.setUser(student2User);
        student2.setStudentCode("D22CQCN002");
        student2.setFullName("Trần Văn Hùng");
        student2.setGender(Gender.MALE);
        student2.setClassCode("D22CQCN01");
        student2.setFacultyCode("CNTT");
        student2.setPriorityCategory(PriorityCategory.NONE);
        student2.setPreviousStayGood(true);
        student2.setConductScore(100);
        student2.setBlockedFromHousing(false);
        studentRepository.save(student2);

        // 6. Seed Student D22CQDT001 (Nữ, DT, D22CQDT01, NONE)
        User student3User = createUser("D22CQDT001", "d22cqdt001@example.com", "Admin@123", Role.STUDENT);
        student3User = userRepository.save(student3User);

        Student student3 = new Student();
        student3.setUser(student3User);
        student3.setStudentCode("D22CQDT001");
        student3.setFullName("Phạm Thị Lan");
        student3.setGender(Gender.FEMALE);
        student3.setClassCode("D22CQDT01");
        student3.setFacultyCode("DT");
        student3.setPriorityCategory(PriorityCategory.NONE);
        student3.setPreviousStayGood(true);
        student3.setConductScore(100);
        student3.setBlockedFromHousing(false);
        studentRepository.save(student3);
    }

    private User createUser(String username, String email, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
