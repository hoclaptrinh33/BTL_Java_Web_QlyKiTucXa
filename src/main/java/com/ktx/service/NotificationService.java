package com.ktx.service;

import java.util.List;

import com.ktx.domain.Notification;
import com.ktx.domain.User;
import com.ktx.domain.enums.NotificationType;

public interface NotificationService {

    Notification notify(User user, String title, String body, NotificationType type);

    boolean sendEmailIfEnabled(Notification notification);

    List<Notification> getNotificationsForUser(Long userId);

    void markAsRead(Long notificationId);
}
