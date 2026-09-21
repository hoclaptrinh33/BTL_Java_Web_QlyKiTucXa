package com.ktx.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Notification;
import com.ktx.domain.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByReadFlagFalse();

    List<Notification> findTop5ByOrderByCreatedAtDesc();

    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
            FROM Notification n
            WHERE n.user.id = :userId
              AND n.type = :type
              AND n.createdAt >= :startOfDay
              AND n.createdAt < :endOfDay
              AND (n.title LIKE CONCAT('%', :contractNo, '%') OR n.body LIKE CONCAT('%', :contractNo, '%'))
            """)
    boolean existsByUserIdAndTypeAndContractNoAndDate(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("contractNo") String contractNo,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
