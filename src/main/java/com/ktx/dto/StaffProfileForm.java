package com.ktx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StaffProfileForm {

    @NotBlank(message = "Họ và tên bắt buộc")
    @Size(max = 120, message = "Họ và tên tối đa 120 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    @Pattern(regexp = "^$|^[+0-9\\s-]{8,20}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
