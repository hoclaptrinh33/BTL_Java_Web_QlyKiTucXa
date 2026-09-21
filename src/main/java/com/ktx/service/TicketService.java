package com.ktx.service;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.enums.TicketPriority;
import com.ktx.domain.enums.TicketStatus;

public interface TicketService {

    MaintenanceTicket createTicket(Long studentId, Long roomId, String title, String description, TicketPriority priority);

    MaintenanceTicket updateStatus(Long ticketId, TicketStatus newStatus, Authentication auth);

    MaintenanceTicket closeTicketByStudent(Long ticketId, Long studentId);

    List<MaintenanceTicket> getTicketsForStudent(Long studentId);

    List<MaintenanceTicket> getTicketsForStaff(Authentication auth, TicketStatus status);

    List<MaintenanceTicket> getTicketsForAdmin(Long buildingId, TicketStatus status);

    int autoCloseResolvedTickets(int days);

    MaintenanceTicket getById(Long ticketId);
}
