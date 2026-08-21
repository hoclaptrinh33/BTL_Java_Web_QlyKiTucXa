package com.ktx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeForm {

    @NotBlank(message = "Mật khẩu hiện tại bắt buộc")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới bắt buộc")
    @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu mới bắt buộc")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
