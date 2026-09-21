package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.enums.TicketPriority;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.MaintenanceTicketRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.impl.TicketServiceImpl;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private MaintenanceTicketRepository ticketRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private StaffScope staffScope;

    private TicketService ticketService;

    private Student student;
    private Room room1;
    private Room room2;
    private Contract contract;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, studentRepository, contractRepository, staffScope);

        student = new Student();
        student.setId(10L);
        student.setFullName("Nguyen Van A");

        Building buildingA = new Building();
        buildingA.setId(1L);
        buildingA.setCode("A");

        Building buildingB = new Building();
        buildingB.setId(2L);
        buildingB.setCode("B");

        room1 = new Room();
        room1.setId(101L);
        room1.setRoomNumber("101");
        room1.setBuilding(buildingA);

        room2 = new Room();
        room2.setId(201L);
        room2.setRoomNumber("201");
        room2.setBuilding(buildingB);

        Bed bed = new Bed();
        bed.setId(5L);
        bed.setRoom(room1);

        contract = new Contract();
        contract.setId(1L);
        contract.setStudent(student);
        contract.setBed(bed);
    }

    @Test
    void createTicket_ownRoom_success() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceTicket ticket = ticketService.createTicket(
                10L, 101L, "Cháy bóng đèn", "Bóng đèn bàn học nhấp nháy rồi tắt", TicketPriority.HIGH);

        assertNotNull(ticket);
        assertEquals(student, ticket.getStudent());
        assertEquals(room1, ticket.getRoom());
        assertEquals("Cháy bóng đèn", ticket.getTitle());
        assertEquals("Bóng đèn bàn học nhấp nháy rồi tắt", ticket.getDescription());
        assertEquals(TicketPriority.HIGH, ticket.getPriority());
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNotNull(ticket.getCreatedAt());
        assertNotNull(ticket.getUpdatedAt());

        verify(ticketRepository, times(1)).save(any(MaintenanceTicket.class));
    }

    @Test
    void createTicket_wrongRoom_throws403Forbidden() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of(contract));

        // Sinh viên phòng 101 cố tình gửi ticket phòng 201 -> 403
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ticketService.createTicket(10L, 201L, "Hỏng điều hòa", "Lỗi phòng khác", TicketPriority.MEDIUM));

        assertEquals("Sinh viên chỉ có thể tạo ticket cho phòng của mình", ex.getMessage());
        verify(ticketRepository, times(0)).save(any(MaintenanceTicket.class));
    }

    @Test
    void createTicket_noActiveRoom_throws403() {
        when(studentRepository.findById(10L)).thenReturn(Optional.of(student));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(10L), anyCollection()))
                .thenReturn(List.of());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ticketService.createTicket(10L, 101L, "Báo hỏng", "Mô tả", TicketPriority.LOW));

        assertEquals("Sinh viên chưa có phòng ký túc xá hợp lệ", ex.getMessage());
        verify(ticketRepository, times(0)).save(any(MaintenanceTicket.class));
    }

    @Test
    void updateStatus_staffScope_ownBuilding_success() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(100L);
        ticket.setRoom(room1);
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication staffAuth = auth("staffA", "ROLE_STAFF");

        MaintenanceTicket updated = ticketService.updateStatus(100L, TicketStatus.IN_PROGRESS, staffAuth);

        assertEquals(TicketStatus.IN_PROGRESS, updated.getStatus());
        verify(staffScope, times(1)).assertRoom(staffAuth, room1);
    }

    @Test
    void updateStatus_staffScope_otherBuilding_throwsDenied() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(100L);
        ticket.setRoom(room2); // Tòa B
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        Authentication staffAuth = auth("staffA", "ROLE_STAFF");
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(staffScope).assertRoom(staffAuth, room2);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ticketService.updateStatus(100L, TicketStatus.IN_PROGRESS, staffAuth));

        assertEquals(StaffScope.DENIED_BUILDING, ex.getMessage());
    }

    @Test
    void closeTicketByStudent_whenResolved_success() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(100L);
        ticket.setStudent(student);
        ticket.setStatus(TicketStatus.RESOLVED);

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceTicket closed = ticketService.closeTicketByStudent(100L, 10L);

        assertEquals(TicketStatus.CLOSED, closed.getStatus());
        verify(ticketRepository, times(1)).save(ticket);
    }

    @Test
    void closeTicketByStudent_notResolved_throwsBusinessException() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(100L);
        ticket.setStudent(student);
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                ticketService.closeTicketByStudent(100L, 10L));

        assertEquals("Chỉ có thể đóng ticket khi đã giải quyết (RESOLVED)", ex.getMessage());
    }

    @Test
    void autoCloseResolvedTickets_closesOldResolvedTickets() {
        MaintenanceTicket oldTicket = new MaintenanceTicket();
        oldTicket.setId(1L);
        oldTicket.setStatus(TicketStatus.RESOLVED);
        oldTicket.setResolvedAt(LocalDateTime.now().minusDays(8));

        MaintenanceTicket recentTicket = new MaintenanceTicket();
        recentTicket.setId(2L);
        recentTicket.setStatus(TicketStatus.RESOLVED);
        recentTicket.setResolvedAt(LocalDateTime.now().minusDays(3));

        when(ticketRepository.findByStatusAndResolvedAtBefore(eq(TicketStatus.RESOLVED), any(LocalDateTime.class)))
                .thenReturn(List.of(oldTicket));

        int closedCount = ticketService.autoCloseResolvedTickets(7);

        assertEquals(1, closedCount);
        assertEquals(TicketStatus.CLOSED, oldTicket.getStatus());
        verify(ticketRepository, times(1)).saveAll(List.of(oldTicket));
    }

    private static Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }
}
