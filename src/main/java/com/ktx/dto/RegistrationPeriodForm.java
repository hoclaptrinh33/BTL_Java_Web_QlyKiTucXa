package com.ktx.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;

public class RegistrationPeriodForm {

    @NotBlank(message = "Tên đợt không được để trống")
    @Size(max = 120, message = "Tên đợt không quá 120 ký tự")
    private String name;

    @NotNull(message = "Loại đợt không được để trống")
    private PeriodType periodType;

    @NotBlank(message = "Năm học không được để trống")
    @Size(max = 20, message = "Năm học không quá 20 ký tự")
    private String academicYear;

    @NotNull(message = "Thời gian mở không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openAt;

    @NotNull(message = "Thời gian đóng không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime closeAt;

    @NotNull(message = "Ngày bắt đầu kỳ học không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate termStart;

    @NotNull(message = "Ngày kết thúc kỳ học không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate termEnd;

    private PeriodStatus status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(LocalDateTime openAt) {
        this.openAt = openAt;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public LocalDate getTermStart() {
        return termStart;
    }

    public void setTermStart(LocalDate termStart) {
        this.termStart = termStart;
    }

    public LocalDate getTermEnd() {
        return termEnd;
    }

    public void setTermEnd(LocalDate termEnd) {
        this.termEnd = termEnd;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
    }
}
