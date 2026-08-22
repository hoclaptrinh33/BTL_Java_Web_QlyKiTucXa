package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ktx.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentCode(String studentCode);

    @Query("SELECT s FROM Student s JOIN FETCH s.user ORDER BY s.studentCode")
    List<Student> findAllWithUser();
}
