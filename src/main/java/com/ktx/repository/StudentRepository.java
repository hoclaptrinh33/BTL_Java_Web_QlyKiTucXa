package com.ktx.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentCode(String studentCode);

    Optional<Student> findByStudentCode(String studentCode);
}
