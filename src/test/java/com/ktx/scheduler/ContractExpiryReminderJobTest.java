package com.ktx.scheduler;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.ktx.domain.Contract;
import com.ktx.domain.Notification;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.User;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.NotificationType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.SystemConfigRepository;

@ExtendWith(MockitoExtension.class)
class ContractExpiryReminderJobTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private ContractExpiryReminderJob job;

    @BeforeEach
    void setUp() {
        job = new ContractExpiryReminderJob(contractRepository, notificationRepository, systemConfigRepository);
    }

    private Contract createTestContract(Long contractId, String contractNo, Long userId, LocalDate endDate) {
        User user = new User();
        user.setId(userId);
        user.setUsername("student_" + userId);

        Student student = new Student();
        student.setId(100L + userId);
        student.setUser(user);
        student.setFullName("Sinh Viên " + userId);

        Contract contract = new Contract();
        contract.setId(contractId);
        contract.setContractNo(contractNo);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setStudent(student);
        contract.setEndDate(endDate);
        return contract;
    }

    @Test
    @DisplayName("HĐ còn 30 ngày xuất hiện notification nhắc hết hạn (mặc định 30 ngày)")
    void remindExpiringContracts_createsNotificationForExpiringContract() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        LocalDate targetEndDate = today.plusDays(30);

        Contract contract = createTestContract(1L, "HD-2026-000001", 10L, targetEndDate);

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.empty()); // fallback to 30
        when(contractRepository.findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate))
                .thenReturn(List.of(contract));
        when(notificationRepository.existsByUserIdAndTypeAndContractNoAndDate(
                eq(10L), eq(NotificationType.CONTRACT_EXPIRY), eq("HD-2026-000001"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        int count = job.remindExpiringContracts(today);

        assertEquals(1, count);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(contract.getStudent().getUser(), saved.getUser());
        assertEquals(NotificationType.CONTRACT_EXPIRY, saved.getType());
        assertFalse(saved.getReadFlag());
        assertFalse(saved.getEmailSent()); // KHONG gui mail o task nay
        assertTrue(saved.getTitle().contains("HD-2026-000001"));
        assertTrue(saved.getBody().contains("HD-2026-000001"));
        assertTrue(saved.getBody().contains(targetEndDate.toString()));
    }

    @Test
    @DisplayName("Job idempotent theo ngày + contract: chạy lại lần 2 trong cùng ngày không sinh thêm notification")
    void remindExpiringContracts_isIdempotentOnSameDay() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        LocalDate targetEndDate = today.plusDays(30);

        Contract contract = createTestContract(1L, "HD-2026-000001", 10L, targetEndDate);

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.empty());
        when(contractRepository.findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate))
                .thenReturn(List.of(contract));

        // Lần 1: chưa có notification
        when(notificationRepository.existsByUserIdAndTypeAndContractNoAndDate(
                eq(10L), eq(NotificationType.CONTRACT_EXPIRY), eq("HD-2026-000001"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        int firstRunCount = job.remindExpiringContracts(today);
        assertEquals(1, firstRunCount);
        verify(notificationRepository, times(1)).save(any(Notification.class));

        // Lần 2 trong cùng ngày: đã có notification
        when(notificationRepository.existsByUserIdAndTypeAndContractNoAndDate(
                eq(10L), eq(NotificationType.CONTRACT_EXPIRY), eq("HD-2026-000001"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        int secondRunCount = job.remindExpiringContracts(today);
        assertEquals(0, secondRunCount); // Không tạo thêm dòng nào

        // Tổng số lần save vẫn là 1
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Đọc số ngày cấu hình tùy biến từ system_configs (ví dụ 15 ngày)")
    void remindExpiringContracts_customRemindDaysConfig() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        LocalDate targetEndDate = today.plusDays(15);

        SystemConfig config = new SystemConfig();
        config.setConfigKey(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS);
        config.setConfigValue("15");

        Contract contract = createTestContract(2L, "HD-2026-000002", 20L, targetEndDate);

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.of(config));
        when(contractRepository.findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate))
                .thenReturn(List.of(contract));
        when(notificationRepository.existsByUserIdAndTypeAndContractNoAndDate(
                eq(20L), eq(NotificationType.CONTRACT_EXPIRY), eq("HD-2026-000002"),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        int count = job.remindExpiringContracts(today);

        assertEquals(1, count);
        verify(contractRepository).findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Giá trị cấu hình không hợp lệ tự động fallback về mặc định 30 ngày")
    void remindExpiringContracts_invalidConfigFallback() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS);
        config.setConfigValue("invalid_number");

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.of(config));

        int remindDays = job.getExpiryRemindDays();
        assertEquals(30, remindDays);
    }

    @Test
    @DisplayName("Bỏ qua hợp đồng thiếu thông tin sinh viên hoặc User")
    void remindExpiringContracts_skipsContractWithMissingStudentOrUser() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        LocalDate targetEndDate = today.plusDays(30);

        Contract contractNoStudent = new Contract();
        contractNoStudent.setContractNo("HD-NO-STUDENT");
        contractNoStudent.setEndDate(targetEndDate);
        contractNoStudent.setStudent(null);

        Contract contractNoUser = new Contract();
        contractNoUser.setContractNo("HD-NO-USER");
        contractNoUser.setEndDate(targetEndDate);
        Student studentNoUser = new Student();
        studentNoUser.setUser(null);
        contractNoUser.setStudent(studentNoUser);

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.empty());
        when(contractRepository.findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate))
                .thenReturn(List.of(contractNoStudent, contractNoUser));

        int count = job.remindExpiringContracts(today);

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Không có hợp đồng nào sắp hết hạn thì trả về 0")
    void remindExpiringContracts_noExpiringContracts() {
        LocalDate today = LocalDate.of(2026, 9, 21);
        LocalDate targetEndDate = today.plusDays(30);

        when(systemConfigRepository.findById(ContractExpiryReminderJob.CONFIG_KEY_EXPIRY_REMIND_DAYS))
                .thenReturn(Optional.empty());
        when(contractRepository.findExpiringContracts(List.of(ContractStatus.ACTIVE), targetEndDate))
                .thenReturn(List.of());

        int count = job.remindExpiringContracts(today);

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Kiểm tra method remindExpiringContracts() có annotation @Scheduled với cron 0 0 8 * * *")
    void remindExpiringContracts_hasScheduledAnnotationWithCorrectCron() throws NoSuchMethodException {
        Method method = ContractExpiryReminderJob.class.getMethod("remindExpiringContracts");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled, "@Scheduled annotation phải tồn tại trên method remindExpiringContracts()");
        assertEquals("0 0 8 * * *", scheduled.cron());
    }
}
