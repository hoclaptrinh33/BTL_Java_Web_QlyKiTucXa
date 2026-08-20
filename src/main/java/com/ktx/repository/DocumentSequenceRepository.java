package com.ktx.repository;

import com.ktx.domain.DocumentSequenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.DocumentSequence;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, DocumentSequenceId> {
}
