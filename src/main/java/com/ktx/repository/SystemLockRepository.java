package com.ktx.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.SystemLock;

import jakarta.persistence.LockModeType;

public interface SystemLockRepository extends JpaRepository<SystemLock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SystemLock s WHERE s.lockName = :lockName")
    Optional<SystemLock> findByLockNameForUpdate(@Param("lockName") String lockName);
}
