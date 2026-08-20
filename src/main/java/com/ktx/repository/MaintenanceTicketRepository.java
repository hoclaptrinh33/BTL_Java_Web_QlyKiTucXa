package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.MaintenanceTicket;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
}
