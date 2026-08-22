package com.ktx.web.admin;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ktx.repository.NotificationRepository;

@ControllerAdvice(assignableTypes = {
        AdminDashboardController.class,
        AdminBuildingController.class,
        AdminRoomController.class,
        AdminStudentController.class,
        AdminPlaceholderController.class
})
public class AdminMenuAdvice {

    private final NotificationRepository notificationRepository;

    public AdminMenuAdvice(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @ModelAttribute("unreadNotifications")
    public long unreadNotifications() {
        return notificationRepository.countByReadFlagFalse();
    }
}
