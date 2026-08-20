package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {
}
