package com.ktx.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;

public class RoomBatchForm {

    @NotNull(message = "Chọn tòa nhà")
    private Long buildingId;

    @NotNull(message = "Tầng từ bắt buộc")
    @Min(value = 1, message = "Tầng từ tối thiểu là 1")
    @Max(value = 30, message = "Tầng từ tối đa 30")
    private Integer floorFrom = 1;

    @NotNull(message = "Tầng đến bắt buộc")
    @Min(value = 1, message = "Tầng đến tối thiểu là 1")
    @Max(value = 30, message = "Tầng đến tối đa 30")
    private Integer floorTo = 5;

    @NotNull(message = "Số phòng mỗi tầng bắt buộc")
    @Min(value = 1, message = "Ít nhất 1 phòng mỗi tầng")
    @Max(value = 20, message = "Tối đa 20 phòng mỗi tầng")
    private Integer roomsPerFloor = 10;

    @NotNull(message = "Loại phòng bắt buộc")
    private RoomType roomType = RoomType.STANDARD_6;

    @NotNull(message = "Giá phòng bắt buộc")
    @DecimalMin(value = "0", message = "Giá phòng không âm")
    private BigDecimal pricePerTerm;

    @NotNull(message = "Trạng thái bắt buộc")
    private RoomStatus status = RoomStatus.ACTIVE;

    @AssertTrue(message = "Tầng đến phải ≥ tầng từ")
    public boolean isFloorRangeValid() {
        if (floorFrom == null || floorTo == null) {
            return true;
        }
        return floorTo >= floorFrom;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Integer getFloorFrom() {
        return floorFrom;
    }

    public void setFloorFrom(Integer floorFrom) {
        this.floorFrom = floorFrom;
    }

    public Integer getFloorTo() {
        return floorTo;
    }

    public void setFloorTo(Integer floorTo) {
        this.floorTo = floorTo;
    }

    public Integer getRoomsPerFloor() {
        return roomsPerFloor;
    }

    public void setRoomsPerFloor(Integer roomsPerFloor) {
        this.roomsPerFloor = roomsPerFloor;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
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
}
