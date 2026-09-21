package com.ktx.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.DocumentSequence;
import com.ktx.domain.DocumentSequenceId;

import jakarta.persistence.LockModeType;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, DocumentSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DocumentSequence d WHERE d.kind = :kind AND d.year = :year")
    Optional<DocumentSequence> findByKindAndYearForUpdate(@Param("kind") String kind, @Param("year") Integer year);
}
