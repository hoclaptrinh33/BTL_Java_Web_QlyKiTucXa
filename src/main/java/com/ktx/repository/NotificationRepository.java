package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByReadFlagFalse();

    List<Notification> findTop5ByOrderByCreatedAtDesc();
}
