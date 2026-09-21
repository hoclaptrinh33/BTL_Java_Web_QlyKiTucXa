package com.ktx.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.DocumentSequence;
import com.ktx.domain.Invoice;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.User;
import com.ktx.repository.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private RoomAssetRepository roomAssetRepository;
    @Mock private RegistrationPeriodRepository registrationPeriodRepository;
    @Mock private RoomApplicationRepository roomApplicationRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private CheckInOutRepository checkInOutRepository;
    @Mock private UtilityReadingRepository utilityReadingRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock private ViolationRepository violationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private DocumentSequenceRepository documentSequenceRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(
                userRepository,
                buildingRepository,
                staffRepository,
                studentRepository,
                roomRepository,
                bedRepository,
                roomAssetRepository,
                registrationPeriodRepository,
                roomApplicationRepository,
                contractRepository,
                checkInOutRepository,
                utilityReadingRepository,
                invoiceRepository,
                invoiceItemRepository,
                paymentRepository,
                maintenanceTicketRepository,
                violationRepository,
                notificationRepository,
                documentSequenceRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Khi user admin đã tồn tại, seeder bỏ qua không ghi đè")
    void testRunWithAdminExisting() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(buildingRepository, never()).save(any());
        verify(roomRepository, never()).save(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Khi cơ sở dữ liệu sạch, seeder khởi tạo đầy đủ dữ liệu 15 phút demo")
    void testRunWithCleanDatabase() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(buildingRepository.save(any(Building.class))).thenAnswer(invocation -> {
            Building b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        when(bedRepository.save(any(Bed.class))).thenAnswer(invocation -> {
            Bed b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice i = invocation.getArgument(0);
            i.setId(1L);
            return i;
        });

        when(registrationPeriodRepository.save(any(RegistrationPeriod.class))).thenAnswer(invocation -> {
            RegistrationPeriod p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        DocumentSequence seq = new DocumentSequence();
        seq.setKind("CONTRACT_NO");
        seq.setYear(2026);
        seq.setLastValue(0);
        when(documentSequenceRepository.findById(any())).thenReturn(Optional.of(seq));

        assertDoesNotThrow(() -> dataSeeder.run());

        // Kiểm tra các entity nòng cốt được lưu
        verify(userRepository, atLeast(10)).save(any(User.class));
        verify(buildingRepository, atLeast(3)).save(any(Building.class));
        verify(roomRepository, atLeast(5)).save(any(Room.class));
        verify(bedRepository, atLeast(20)).save(any(Bed.class));
        verify(contractRepository, atLeast(10)).save(any(Contract.class));
        verify(invoiceRepository, atLeast(8)).save(any(Invoice.class));
        verify(registrationPeriodRepository, atLeast(2)).save(any(RegistrationPeriod.class));
        verify(roomApplicationRepository, atLeast(6)).save(any());
        verify(utilityReadingRepository, atLeast(2)).save(any());
        verify(maintenanceTicketRepository, atLeast(2)).save(any());
        verify(violationRepository, atLeast(1)).save(any());
        verify(notificationRepository, atLeast(3)).save(any());
    }
}
