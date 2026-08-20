package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
