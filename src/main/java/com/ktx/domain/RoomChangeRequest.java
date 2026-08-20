package com.ktx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;

@Entity
@Table(name = "room_change_requests")
public class RoomChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoomChangeKind requestKind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_bed_id", nullable = false)
    private Bed currentBed;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "requested_building_id", nullable = true)
    private Building requestedBuilding;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private RoomType requestedRoomType;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "target_bed_id", nullable = true)
    private Bed targetBed;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoomChangeStatus status;

    @Column(length = 500)
    private String adminNote;

    public RoomChangeRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public RoomChangeKind getRequestKind() {
        return requestKind;
    }

    public void setRequestKind(RoomChangeKind requestKind) {
        this.requestKind = requestKind;
    }

    public Bed getCurrentBed() {
        return currentBed;
    }

    public void setCurrentBed(Bed currentBed) {
        this.currentBed = currentBed;
    }

    public Building getRequestedBuilding() {
        return requestedBuilding;
    }

    public void setRequestedBuilding(Building requestedBuilding) {
        this.requestedBuilding = requestedBuilding;
    }

    public RoomType getRequestedRoomType() {
        return requestedRoomType;
    }

    public void setRequestedRoomType(RoomType requestedRoomType) {
        this.requestedRoomType = requestedRoomType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Bed getTargetBed() {
        return targetBed;
    }

    public void setTargetBed(Bed targetBed) {
        this.targetBed = targetBed;
    }

    public RoomChangeStatus getStatus() {
        return status;
    }

    public void setStatus(RoomChangeStatus status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}
