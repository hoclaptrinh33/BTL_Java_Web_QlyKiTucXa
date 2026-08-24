package com.ktx.dto;

import java.util.List;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;

public class RoomDiagramDto {
    private Long id;
    private String roomNumber;
    private String doorCode;
    private int floor;
    private RoomType roomType;
    private String typeLabel;
    private RoomStatus status;
    private int capacity;
    private long occupiedCount;
    private List<BedDiagramDto> beds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public long getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(long occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    public List<BedDiagramDto> getBeds() {
        return beds;
    }

    public void setBeds(List<BedDiagramDto> beds) {
        this.beds = beds;
    }
}
