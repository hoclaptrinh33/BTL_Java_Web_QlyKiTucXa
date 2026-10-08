package com.ktx.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT a FROM AuditLog a WHERE "
            + "(:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + " OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + " OR LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + " OR LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:action IS NULL OR a.action = :action) "
            + "AND (:targetType IS NULL OR a.targetType = :targetType) "
            + "AND (:status IS NULL OR a.status = :status) "
            + "AND (:from IS NULL OR a.createdAt >= :from) "
            + "AND (:to IS NULL OR a.createdAt <= :to) "
            + "ORDER BY a.createdAt DESC")
    Page<AuditLog> findWithFilters(@Param("keyword") String keyword,
                                  @Param("action") String action,
                                  @Param("targetType") String targetType,
                                  @Param("status") String status,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByActionStartingWithAndCreatedAtAfter(String actionPrefix, LocalDateTime dateTime);

    long countByStatusAndCreatedAtAfter(String status, LocalDateTime dateTime);
}
