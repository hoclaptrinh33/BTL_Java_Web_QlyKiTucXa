package com.ktx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.ktx.domain.enums.PeriodGenderScope;
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

    // --- Các trường thông tin nâng cấp ---

    @NotNull(message = "Phạm vi giới tính không được để trống")
    private PeriodGenderScope genderScope = PeriodGenderScope.ALL;

    @Min(value = 0, message = "Điểm rèn luyện tối thiểu không nhỏ hơn 0")
    @Max(value = 100, message = "Điểm rèn luyện tối thiểu không lớn hơn 100")
    private Integer minConductScore = 0;

    @Size(max = 100, message = "Khóa/Đối tượng áp dụng không quá 100 ký tự")
    private String targetCohort;

    @Min(value = 1, message = "Chỉ tiêu dự kiến phải lớn hơn 0")
    private Integer targetQuota;

    private List<Long> buildingIds = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime paymentDeadline;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkinStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkinEnd;

    @DecimalMin(value = "0.00", message = "Tỷ lệ đặt cọc tối thiểu là 0%")
    @DecimalMax(value = "1.00", message = "Tỷ lệ đặt cọc tối đa là 100%")
    private BigDecimal depositRatio = BigDecimal.valueOf(0.50);

    private String paymentGuide;

    private Boolean requireDocumentProof = false;

    private String termsAndConditions;

    private String description;

    @Size(max = 30, message = "Hotline liên hệ không quá 30 ký tự")
    private String contactPhone;

    @Email(message = "Email liên hệ không đúng định dạng")
    @Size(max = 100, message = "Email liên hệ không quá 100 ký tự")
    private String contactEmail;

    public RegistrationPeriodForm() {
    }

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

    public PeriodGenderScope getGenderScope() {
        return genderScope;
    }

    public void setGenderScope(PeriodGenderScope genderScope) {
        this.genderScope = genderScope;
    }

    public Integer getMinConductScore() {
        return minConductScore;
    }

    public void setMinConductScore(Integer minConductScore) {
        this.minConductScore = minConductScore;
    }

    public String getTargetCohort() {
        return targetCohort;
    }

    public void setTargetCohort(String targetCohort) {
        this.targetCohort = targetCohort;
    }

    public Integer getTargetQuota() {
        return targetQuota;
    }

    public void setTargetQuota(Integer targetQuota) {
        this.targetQuota = targetQuota;
    }

    public List<Long> getBuildingIds() {
        return buildingIds;
    }

    public void setBuildingIds(List<Long> buildingIds) {
        this.buildingIds = buildingIds;
    }

    public LocalDateTime getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public LocalDate getCheckinStart() {
        return checkinStart;
    }

    public void setCheckinStart(LocalDate checkinStart) {
        this.checkinStart = checkinStart;
    }

    public LocalDate getCheckinEnd() {
        return checkinEnd;
    }

    public void setCheckinEnd(LocalDate checkinEnd) {
        this.checkinEnd = checkinEnd;
    }

    public BigDecimal getDepositRatio() {
        return depositRatio;
    }

    public void setDepositRatio(BigDecimal depositRatio) {
        this.depositRatio = depositRatio;
    }

    public String getPaymentGuide() {
        return paymentGuide;
    }

    public void setPaymentGuide(String paymentGuide) {
        this.paymentGuide = paymentGuide;
    }

    public Boolean getRequireDocumentProof() {
        return requireDocumentProof;
    }

    public void setRequireDocumentProof(Boolean requireDocumentProof) {
        this.requireDocumentProof = requireDocumentProof;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
