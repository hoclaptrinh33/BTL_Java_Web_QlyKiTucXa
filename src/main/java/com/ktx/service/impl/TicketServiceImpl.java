package com.ktx.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
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
import com.ktx.service.TicketService;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final StaffScope staffScope;

    public TicketServiceImpl(MaintenanceTicketRepository maintenanceTicketRepository,
                             StudentRepository studentRepository,
                             ContractRepository contractRepository,
                             StaffScope staffScope) {
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
        this.staffScope = staffScope;
    }

    @Override
    public MaintenanceTicket createTicket(Long studentId, Long roomId, String title, String description, TicketPriority priority) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên"));

        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                studentId, OccupyingStatuses.OCCUPYING);
        if (contracts.isEmpty()) {
            throw new AccessDeniedException("Sinh viên chưa có phòng ký túc xá hợp lệ");
        }

        Room studentRoom = contracts.get(0).getBed().getRoom();
        if (roomId != null && !studentRoom.getId().equals(roomId)) {
            throw new AccessDeniedException("Sinh viên chỉ có thể tạo ticket cho phòng của mình");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("Tiêu đề không được để trống");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessException("Mô tả không được để trống");
        }

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setStudent(student);
        ticket.setRoom(studentRoom);
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(priority != null ? priority : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);

        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        return maintenanceTicketRepository.save(ticket);
    }

    @Override
    public MaintenanceTicket updateStatus(Long ticketId, TicketStatus newStatus, Authentication auth) {
        MaintenanceTicket ticket = getById(ticketId);
        staffScope.assertRoom(auth, ticket.getRoom());

        if (newStatus == null) {
            throw new BusinessException("Trạng thái mới không hợp lệ");
        }

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        } else if (newStatus != TicketStatus.RESOLVED && newStatus != TicketStatus.CLOSED) {
            ticket.setResolvedAt(null);
        }

        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(now);

        return maintenanceTicketRepository.save(ticket);
    }

    @Override
    public MaintenanceTicket closeTicketByStudent(Long ticketId, Long studentId) {
        MaintenanceTicket ticket = getById(ticketId);
        if (!ticket.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Không có quyền đóng ticket này");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BusinessException("Chỉ có thể đóng ticket khi đã giải quyết (RESOLVED)");
        }

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setUpdatedAt(LocalDateTime.now());
        return maintenanceTicketRepository.save(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getTicketsForStudent(Long studentId) {
        return maintenanceTicketRepository.findByStudentIdWithDetails(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getTicketsForStaff(Authentication auth, TicketStatus status) {
        Optional<Long> buildingIdOpt = staffScope.buildingId(auth);
        List<MaintenanceTicket> list;
        if (buildingIdOpt.isPresent()) {
            list = maintenanceTicketRepository.findByBuildingIdWithDetails(buildingIdOpt.get());
        } else {
            list = maintenanceTicketRepository.findAllWithDetails();
        }

        if (status != null) {
            return list.stream().filter(t -> t.getStatus() == status).toList();
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getTicketsForAdmin(Long buildingId, TicketStatus status) {
        List<MaintenanceTicket> list;
        if (buildingId != null) {
            list = maintenanceTicketRepository.findByBuildingIdWithDetails(buildingId);
        } else {
            list = maintenanceTicketRepository.findAllWithDetails();
        }

        if (status != null) {
            return list.stream().filter(t -> t.getStatus() == status).toList();
        }
        return list;
    }

    @Override
    public int autoCloseResolvedTickets(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<MaintenanceTicket> list = maintenanceTicketRepository.findByStatusAndResolvedAtBefore(
                TicketStatus.RESOLVED, cutoff);
        LocalDateTime now = LocalDateTime.now();
        for (MaintenanceTicket t : list) {
            t.setStatus(TicketStatus.CLOSED);
            t.setUpdatedAt(now);
        }
        maintenanceTicketRepository.saveAll(list);
        return list.size();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceTicket getById(Long ticketId) {
        return maintenanceTicketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy ticket #" + ticketId));
    }
}
