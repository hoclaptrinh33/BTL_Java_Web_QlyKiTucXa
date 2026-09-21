package com.ktx.dto;

import com.ktx.domain.UtilityReading;

public class RoomReadingDto {
    private Long roomId;
    private String roomNumber;
    private Integer floor;
    private String roomTypeName;
    private int occupyingCount;
    private UtilityReading reading;
    private Integer kwh;
    private Integer m3;
    private boolean hasReading;
    private boolean hasActiveInvoice;

    public RoomReadingDto() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public int getOccupyingCount() {
        return occupyingCount;
    }

    public void setOccupyingCount(int occupyingCount) {
        this.occupyingCount = occupyingCount;
    }

    public UtilityReading getReading() {
        return reading;
    }

    public void setReading(UtilityReading reading) {
        this.reading = reading;
    }

    public Integer getKwh() {
        return kwh;
    }

    public void setKwh(Integer kwh) {
        this.kwh = kwh;
    }

    public Integer getM3() {
        return m3;
    }

    public void setM3(Integer m3) {
        this.m3 = m3;
    }

    public boolean isHasReading() {
        return hasReading;
    }

    public void setHasReading(boolean hasReading) {
        this.hasReading = hasReading;
    }

    public boolean isHasActiveInvoice() {
        return hasActiveInvoice;
    }

    public void setHasActiveInvoice(boolean hasActiveInvoice) {
        this.hasActiveInvoice = hasActiveInvoice;
    }
}
