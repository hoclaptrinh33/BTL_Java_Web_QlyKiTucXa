package com.ktx.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ktx.config.MailConfig;
import com.ktx.domain.Notification;
import com.ktx.domain.User;
import com.ktx.domain.enums.NotificationType;
import com.ktx.repository.NotificationRepository;
import com.ktx.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MailConfig mailConfig;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private NotificationService notificationService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("sv001");
        sampleUser.setEmail("sv001@example.com");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getId() == null) {
                n.setId(100L);
            }
            return n;
        });
    }

    @Test
    @DisplayName("When mail is disabled (opt-in default), creates notification without sending email")
    void notify_whenMailDisabled_doesNotSendEmail() {
        when(mailConfig.isMailEnabled()).thenReturn(false);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        notificationService = new NotificationServiceImpl(notificationRepository, mailConfig, mailSenderProvider, "noreply@ktx.edu.vn");

        Notification result = notificationService.notify(sampleUser, "Tiêu đề", "Nội dung", NotificationType.CONTRACT_EXPIRY);

        assertNotNull(result);
        assertEquals("Tiêu đề", result.getTitle());
        assertEquals("Nội dung", result.getBody());
        assertEquals(NotificationType.CONTRACT_EXPIRY, result.getType());
        assertFalse(result.getEmailSent());
        assertFalse(result.getReadFlag());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("When mail is enabled, sends email and marks emailSent true")
    void notify_whenMailEnabled_sendsEmailSuccessfully() {
        when(mailConfig.isMailEnabled()).thenReturn(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        notificationService = new NotificationServiceImpl(notificationRepository, mailConfig, mailSenderProvider, "noreply@ktx.edu.vn");

        Notification result = notificationService.notify(sampleUser, "Thông báo hợp đồng", "Nội dung chi tiết", NotificationType.CONTRACT_EXPIRY);

        assertNotNull(result);
        assertTrue(result.getEmailSent());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals("noreply@ktx.edu.vn", sentMessage.getFrom());
        assertEquals("sv001@example.com", sentMessage.getTo()[0]);
        assertEquals("Thông báo hợp đồng", sentMessage.getSubject());
        assertEquals("Nội dung chi tiết", sentMessage.getText());
    }

    @Test
    @DisplayName("When mail sending fails with exception, application continues gracefully without failing notification")
    void notify_whenMailThrowsException_gracefullyHandlesWithoutFailing() {
        when(mailConfig.isMailEnabled()).thenReturn(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        notificationService = new NotificationServiceImpl(notificationRepository, mailConfig, mailSenderProvider, "noreply@ktx.edu.vn");

        doThrow(new MailSendException("SMTP connection failed"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        Notification result = notificationService.notify(sampleUser, "Thông báo", "Nội dung", NotificationType.GENERIC);

        assertNotNull(result);
        assertFalse(result.getEmailSent());
        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("When javaMailSender bean is null (no SMTP config), gracefully skips mail")
    void notify_whenMailSenderIsNull_skipsMail() {
        when(mailConfig.isMailEnabled()).thenReturn(true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        notificationService = new NotificationServiceImpl(notificationRepository, mailConfig, mailSenderProvider, "noreply@ktx.edu.vn");

        Notification result = notificationService.notify(sampleUser, "Thông báo", "Nội dung", NotificationType.GENERIC);

        assertNotNull(result);
        assertFalse(result.getEmailSent());
    }

    @Test
    @DisplayName("markAsRead updates readFlag to true")
    void markAsRead_updatesFlag() {
        Notification notification = new Notification();
        notification.setId(100L);
        notification.setReadFlag(false);

        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        notificationService = new NotificationServiceImpl(notificationRepository, mailConfig, mailSenderProvider, "noreply@ktx.edu.vn");

        notificationService.markAsRead(100L);

        assertTrue(notification.getReadFlag());
        verify(notificationRepository, times(1)).save(notification);
    }
}
