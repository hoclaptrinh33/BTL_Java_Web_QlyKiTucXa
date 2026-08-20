package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.RoomChangeRequest;

public interface RoomChangeRequestRepository extends JpaRepository<RoomChangeRequest, Long> {
}
