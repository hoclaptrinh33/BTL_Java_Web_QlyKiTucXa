package com.ktx.dto;

import java.math.BigDecimal;

import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;

public class RoomRow {

    private Long id;
    private Long buildingId;
    private String buildingCode;
    private String buildingName;
    private String genderLabel;
    private String roomNumber;
    private String doorCode;
    private int floor;
    private RoomType roomType;
    private String typeLabel;
    private int capacity;
    private BigDecimal pricePerTerm;
    private RoomStatus status;
    private String statusLabel;
    private String statusClass;
    private long occupiedBeds;
    private long vacantBeds;
    private long maintenanceBeds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getGenderLabel() {
        return genderLabel;
    }

    public void setGenderLabel(String genderLabel) {
        this.genderLabel = genderLabel;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getDoorCode() {
        return doorCode;
    }

    public void setDoorCode(String doorCode) {
        this.doorCode = doorCode;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPricePerTerm() {
        return pricePerTerm;
    }

    public void setPricePerTerm(BigDecimal pricePerTerm) {
        this.pricePerTerm = pricePerTerm;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public long getOccupiedBeds() {
        return occupiedBeds;
    }

    public void setOccupiedBeds(long occupiedBeds) {
        this.occupiedBeds = occupiedBeds;
    }

    public long getVacantBeds() {
        return vacantBeds;
    }

    public void setVacantBeds(long vacantBeds) {
        this.vacantBeds = vacantBeds;
    }

    public long getMaintenanceBeds() {
        return maintenanceBeds;
    }

    public void setMaintenanceBeds(long maintenanceBeds) {
        this.maintenanceBeds = maintenanceBeds;
    }
}
