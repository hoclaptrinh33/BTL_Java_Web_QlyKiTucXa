package com.ktx.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ktx.domain.enums.AssetCategory;
import com.ktx.domain.enums.AssetCondition;

public class RoomAssetForm {

    @NotBlank(message = "Tên tài sản bắt buộc")
    @Size(max = 120, message = "Tên tối đa 120 ký tự")
    private String name;

    @NotNull(message = "Loại tài sản bắt buộc")
    private AssetCategory category;

    @NotNull(message = "Số lượng bắt buộc")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer quantity = 1;

    @NotNull(message = "Tình trạng bắt buộc")
    private AssetCondition condition = AssetCondition.GOOD;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    @Size(max = 50, message = "Số serial tối đa 50 ký tự")
    private String serialNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetCategory getCategory() {
        return category;
    }

    public void setCategory(AssetCategory category) {
        this.category = category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public AssetCondition getCondition() {
        return condition;
    }

    public void setCondition(AssetCondition condition) {
        this.condition = condition;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
