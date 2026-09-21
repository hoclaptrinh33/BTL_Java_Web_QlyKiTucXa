package com.ktx.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.config.MailConfig;
import com.ktx.domain.Notification;
import com.ktx.domain.User;
import com.ktx.domain.enums.NotificationType;
import com.ktx.repository.NotificationRepository;
import com.ktx.service.NotificationService;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final MailConfig mailConfig;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   MailConfig mailConfig,
                                   ObjectProvider<JavaMailSender> mailSenderProvider,
                                   @Value("${spring.mail.username:noreply@ktx.edu.vn}") String fromAddress) {
        this.notificationRepository = notificationRepository;
        this.mailConfig = mailConfig;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
    }

    @Override
    public Notification notify(User user, String title, String body, NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type != null ? type : NotificationType.GENERIC);
        notification.setReadFlag(false);
        notification.setEmailSent(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);
        sendEmailIfEnabled(saved);
        return saved;
    }

    @Override
    public boolean sendEmailIfEnabled(Notification notification) {
        if (!mailConfig.isMailEnabled()) {
            log.debug("Mail service bị tắt (ktx.mail.enabled=false). Bỏ qua gửi email.");
            return false;
        }

        if (mailSender == null) {
            log.warn("ktx.mail.enabled=true nhưng không tìm thấy cấu hình JavaMailSender. Bỏ qua gửi email.");
            return false;
        }

        if (notification == null || notification.getUser() == null || notification.getUser().getEmail() == null
                || notification.getUser().getEmail().isBlank()) {
            log.warn("Không thể gửi email vì thông tin người dùng hoặc địa chỉ email bị thiếu.");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(notification.getUser().getEmail());
            message.setSubject(notification.getTitle());
            message.setText(notification.getBody());

            mailSender.send(message);
            notification.setEmailSent(true);
            notificationRepository.save(notification);
            log.info("Đã gửi email thông báo thành công tới {}", notification.getUser().getEmail());
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo tới {}: {}", notification.getUser().getEmail(), e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setReadFlag(true);
            notificationRepository.save(n);
        });
    }
}
