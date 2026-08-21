package com.ktx.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.ktx.domain.enums.Gender;

public class RegisterForm {

    @NotBlank(message = "Họ và tên bắt buộc")
    @Size(max = 120)
    private String fullName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email bắt buộc")
    @Email(message = "Email không hợp lệ")
    @Size(max = 120)
    private String email;

    @Size(max = 20)
    private String phone;

    @NotNull(message = "Giới tính bắt buộc")
    private Gender gender;

    @NotBlank(message = "MSSV bắt buộc")
    @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "MSSV gồm 8–12 ký tự chữ và số")
    private String studentCode;

    @NotBlank(message = "Mật khẩu bắt buộc")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu bắt buộc")
    private String confirmPassword;

    @AssertTrue(message = "Cần đồng ý điều khoản sử dụng")
    private boolean termsAccepted;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }
}
