package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Invoice;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.Room;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.dto.DashboardSnapshot;
import com.ktx.dto.DebtByMonthDto;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.MaintenanceTicketRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private RoomApplicationRepository roomApplicationRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private RegistrationPeriodRepository registrationPeriodRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                studentRepository,
                buildingRepository,
                roomRepository,
                bedRepository,
                contractRepository,
                roomApplicationRepository,
                notificationRepository,
                maintenanceTicketRepository,
                invoiceRepository,
                registrationPeriodRepository
        );
    }

    @Test
    void testOccupancyFormula_CompliesWithSpec() {
        // Spec: Occupancy = COUNT(bed OCCUPIED) / COUNT(bed thuộc Room.status=ACTIVE và bed KHÔNG MAINTENANCE)
        // Phòng INACTIVE không vào mẫu số.
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        // Room 1: ACTIVE, 4 beds: 2 OCCUPIED, 1 VACANT, 1 MAINTENANCE
        Room rActive = new Room();
        rActive.setId(1L);
        rActive.setBuilding(b);
        rActive.setRoomNumber("A-101");
        rActive.setStatus(RoomStatus.ACTIVE);

        Bed bed1 = new Bed(); bed1.setId(1L); bed1.setRoom(rActive); bed1.setStatus(BedStatus.OCCUPIED);
        Bed bed2 = new Bed(); bed2.setId(2L); bed2.setRoom(rActive); bed2.setStatus(BedStatus.OCCUPIED);
        Bed bed3 = new Bed(); bed3.setId(3L); bed3.setRoom(rActive); bed3.setStatus(BedStatus.VACANT);
        Bed bed4 = new Bed(); bed4.setId(4L); bed4.setRoom(rActive); bed4.setStatus(BedStatus.MAINTENANCE);

        // Room 2: MAINTENANCE, 2 beds: 1 MAINTENANCE, 1 VACANT
        Room rMaint = new Room();
        rMaint.setId(2L);
        rMaint.setBuilding(b);
        rMaint.setRoomNumber("A-102");
        rMaint.setStatus(RoomStatus.MAINTENANCE);

        Bed bed5 = new Bed(); bed5.setId(5L); bed5.setRoom(rMaint); bed5.setStatus(BedStatus.MAINTENANCE);
        Bed bed6 = new Bed(); bed6.setId(6L); bed6.setRoom(rMaint); bed6.setStatus(BedStatus.VACANT);

        // Room 3: INACTIVE, 2 beds: 2 VACANT
        Room rInactive = new Room();
        rInactive.setId(3L);
        rInactive.setBuilding(b);
        rInactive.setRoomNumber("A-103");
        rInactive.setStatus(RoomStatus.INACTIVE);

        Bed bed7 = new Bed(); bed7.setId(7L); bed7.setRoom(rInactive); bed7.setStatus(BedStatus.VACANT);
        Bed bed8 = new Bed(); bed8.setId(8L); bed8.setRoom(rInactive); bed8.setStatus(BedStatus.VACANT);

        when(buildingRepository.findAll()).thenReturn(List.of(b));
        when(roomRepository.findAllWithBuilding()).thenReturn(List.of(rActive, rMaint, rInactive));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed1, bed2, bed3, bed4, bed5, bed6, bed7, bed8));

        DashboardSnapshot snap = dashboardService.load();

        // Numerator: 2 occupied beds (bed1, bed2)
        assertEquals(2, snap.getOccupiedBeds());
        // Vacant in active rooms: 1 (bed3)
        assertEquals(1, snap.getVacantBeds());
        // Capacity in active rooms: 2 occupied + 1 vacant = 3
        assertEquals(3, snap.getCapacityBeds());
        // Occupancy percent: 2 / 3 * 100 = 66.7%
        assertEquals(66.7, snap.getOccupancyPercent(), 0.05);

        // Check building occupancy
        assertEquals(1, snap.getBuildings().size());
        assertEquals(66.7, snap.getBuildings().get(0).getOccupancyPercent(), 0.05);
    }

    @Test
    void testOpenPeriodAndOperationalAlerts() {
        RegistrationPeriod p = new RegistrationPeriod();
        p.setId(10L);
        p.setName("Đợt Tân Sinh Viên 2026");
        p.setStatus(PeriodStatus.OPEN);

        when(registrationPeriodRepository.findByStatus(PeriodStatus.OPEN)).thenReturn(List.of(p));
        when(roomApplicationRepository.countByPeriodIdAndStatus(10L, ApplicationStatus.SUBMITTED)).thenReturn(15L);
        when(roomApplicationRepository.countByPeriodIdAndStatus(10L, ApplicationStatus.ALLOCATED)).thenReturn(10L);
        when(roomApplicationRepository.countByPeriodIdAndStatus(10L, ApplicationStatus.WAITLISTED)).thenReturn(5L);

        when(maintenanceTicketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(7L);
        when(contractRepository.countExpiringContracts(any(), any(), any())).thenReturn(4L);
        when(invoiceRepository.countByStatus(InvoiceStatus.OVERDUE)).thenReturn(3L);

        DashboardSnapshot snap = dashboardService.load();

        assertTrue(snap.isHasOpenPeriod());
        assertEquals("Đợt Tân Sinh Viên 2026", snap.getOpenPeriodName());
        assertEquals(15L, snap.getOpenPeriodSubmittedCount());
        assertEquals(10L, snap.getOpenPeriodAllocatedCount());
        assertEquals(5L, snap.getOpenPeriodWaitlistedCount());

        assertEquals(7L, snap.getOpenTicketCount());
        assertEquals(4L, snap.getExpiringContractCount());
        assertEquals(3L, snap.getOverdueInvoiceCount());

        // Operational notices should be present
        assertTrue(snap.getNotices().stream().anyMatch(n -> n.getTitle().contains("sắp hết hạn")));
        assertTrue(snap.getNotices().stream().anyMatch(n -> n.getTitle().contains("quá hạn")));
        assertTrue(snap.getNotices().stream().anyMatch(n -> n.getTitle().contains("sửa chữa")));
    }

    @Test
    void testCalculateDebtByMonth() {
        Invoice inv1 = new Invoice();
        inv1.setBillingMonth(LocalDate.of(2026, 8, 1));
        inv1.setTotal(new BigDecimal("1000000"));
        inv1.setStatus(InvoiceStatus.UNPAID);

        Invoice inv2 = new Invoice();
        inv2.setBillingMonth(LocalDate.of(2026, 9, 1));
        inv2.setTotal(new BigDecimal("2000000"));
        inv2.setStatus(InvoiceStatus.OVERDUE);

        Invoice inv3 = new Invoice();
        inv3.setDueDate(LocalDate.of(2026, 9, 15));
        inv3.setTotal(new BigDecimal("1500000"));
        inv3.setStatus(InvoiceStatus.OVERDUE);

        when(invoiceRepository.findByStatusInOrderByDueDateAsc(any())).thenReturn(List.of(inv1, inv2, inv3));

        DebtByMonthDto dto = dashboardService.calculateDebtByMonth();

        assertEquals(3, dto.getTotalInvoices());
        assertEquals(new BigDecimal("4500000"), dto.getTotalDebt());
        assertEquals(2, dto.getLabels().size());
        assertEquals("08/2026", dto.getLabels().get(0));
        assertEquals("09/2026", dto.getLabels().get(1));
        assertEquals(new BigDecimal("1000000"), dto.getData().get(0));
        assertEquals(new BigDecimal("3500000"), dto.getData().get(1));
    }

    @Test
    void testGetOccupancyChartData() {
        Building b = new Building();
        b.setId(1L);
        b.setCode("B");
        b.setName("Tòa B");
        b.setGenderPolicy(BuildingGenderPolicy.FEMALE);

        Room r = new Room();
        r.setId(1L);
        r.setBuilding(b);
        r.setStatus(RoomStatus.ACTIVE);

        Bed bed1 = new Bed(); bed1.setId(1L); bed1.setRoom(r); bed1.setStatus(BedStatus.OCCUPIED);
        Bed bed2 = new Bed(); bed2.setId(2L); bed2.setRoom(r); bed2.setStatus(BedStatus.VACANT);

        when(buildingRepository.findAll()).thenReturn(List.of(b));
        when(roomRepository.findAllWithBuilding()).thenReturn(List.of(r));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed1, bed2));

        Map<String, Object> data = dashboardService.getOccupancyChartData();
        assertNotNull(data);
        assertTrue(data.containsKey("system"));
        assertTrue(data.containsKey("buildings"));

        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) data.get("system");
        assertEquals(1L, system.get("occupied"));
        assertEquals(1L, system.get("vacant"));
        assertEquals(50.0, system.get("occupancyPercent"));
    }

    @Test
    void testLoadForBuilding() {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        Room r = new Room();
        r.setId(1L);
        r.setBuilding(b);
        r.setStatus(RoomStatus.ACTIVE);

        Bed bed = new Bed();
        bed.setId(1L);
        bed.setRoom(r);
        bed.setStatus(BedStatus.OCCUPIED);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(roomRepository.findAllWithBuilding()).thenReturn(List.of(r));
        when(bedRepository.findAllWithRoomAndBuilding()).thenReturn(List.of(bed));
        when(maintenanceTicketRepository.countByStatusAndRoomBuildingId(TicketStatus.OPEN, 1L)).thenReturn(2L);

        DashboardSnapshot snap = dashboardService.loadForBuilding(1L);

        assertTrue(snap.isStaffView());
        assertEquals("Tòa A", snap.getBuildingName());
        assertEquals("A", snap.getBuildingCode());
        assertEquals(1, snap.getOccupiedBeds());
        assertEquals(100.0, snap.getOccupancyPercent(), 0.1);
        assertEquals(2L, snap.getOpenTicketCount());
    }
}
