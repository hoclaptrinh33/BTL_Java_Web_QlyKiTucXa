package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentCode(String studentCode);

    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.user.username = :username")
    Optional<Student> findByUserUsername(@Param("username") String username);

    @Query("SELECT s FROM Student s JOIN FETCH s.user ORDER BY s.studentCode")
    List<Student> findAllWithUser();
}
