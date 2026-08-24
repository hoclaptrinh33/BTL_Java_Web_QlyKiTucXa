package com.ktx.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StudentProfileForm {

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    @Pattern(regexp = "^$|^[+0-9\\s-]{8,20}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 120, message = "Tên người liên hệ khẩn cấp tối đa 120 ký tự")
    private String emergencyName;

    @Size(max = 20, message = "SĐT người liên hệ khẩn cấp tối đa 20 ký tự")
    @Pattern(regexp = "^$|^[+0-9\\s-]{8,20}$", message = "Số điện thoại khẩn cấp không hợp lệ")
    private String emergencyPhone;

    @Size(max = 120, message = "Quê quán tối đa 120 ký tự")
    private String hometown;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmergencyName() {
        return emergencyName;
    }

    public void setEmergencyName(String emergencyName) {
        this.emergencyName = emergencyName;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }
}
