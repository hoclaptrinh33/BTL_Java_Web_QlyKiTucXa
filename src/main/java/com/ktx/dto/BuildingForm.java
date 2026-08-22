package com.ktx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ktx.domain.enums.BuildingGenderPolicy;

public class BuildingForm {

    @NotBlank(message = "Mã tòa bắt buộc")
    @Size(max = 10, message = "Mã tòa tối đa 10 ký tự")
    @Pattern(regexp = "[A-Za-z0-9]{1,10}", message = "Mã tòa gồm chữ và số, không dấu cách")
    private String code;

    @NotBlank(message = "Tên tòa bắt buộc")
    @Size(max = 120, message = "Tên tòa tối đa 120 ký tự")
    private String name;

    @NotNull(message = "Giới tính tòa bắt buộc")
    private BuildingGenderPolicy genderPolicy;

    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BuildingGenderPolicy getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(BuildingGenderPolicy genderPolicy) {
        this.genderPolicy = genderPolicy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
