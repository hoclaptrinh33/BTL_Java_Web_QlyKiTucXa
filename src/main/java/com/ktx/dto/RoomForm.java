package com.ktx.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;

public class RoomForm {

    @NotNull(message = "Chọn tòa nhà")
    private Long buildingId;

    @NotBlank(message = "Số phòng bắt buộc")
    @Size(max = 10, message = "Số phòng tối đa 10 ký tự")
    @Pattern(regexp = "[A-Za-z0-9-]{1,10}", message = "Số phòng gồm chữ, số hoặc gạch ngang")
    private String roomNumber;

    @NotNull(message = "Tầng bắt buộc")
    @Min(value = 1, message = "Tầng tối thiểu là 1")
    private Integer floor;

    @NotNull(message = "Loại phòng bắt buộc")
    private RoomType roomType;

    @NotNull(message = "Giá phòng bắt buộc")
    @DecimalMin(value = "0", message = "Giá phòng không âm")
    private BigDecimal pricePerTerm;

    @NotNull(message = "Trạng thái bắt buộc")
    private RoomStatus status = RoomStatus.ACTIVE;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
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
