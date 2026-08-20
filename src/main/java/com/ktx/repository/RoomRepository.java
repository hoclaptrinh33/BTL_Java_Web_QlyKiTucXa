package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
