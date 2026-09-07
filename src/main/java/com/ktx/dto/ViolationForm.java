package com.ktx.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;

public class ViolationForm {

    @NotNull(message = "Vui lòng chọn sinh viên")
    private Long studentId;

    @NotNull(message = "Vui lòng chọn loại vi phạm")
    private ViolationType violationType;

    @NotNull(message = "Vui lòng chọn mức độ")
    private ViolationSeverity severity;

    @Min(value = 0, message = "Điểm trừ không được âm")
    private Integer pointsDeducted;

    @NotNull(message = "Vui lòng chọn hành động")
    private ViolationAction action;

    @Size(max = 2000, message = "Mô tả không quá 2000 ký tự")
    private String description;

    private LocalDateTime occurredAt;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    public void setViolationType(ViolationType violationType) {
        this.violationType = violationType;
    }

    public ViolationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ViolationSeverity severity) {
        this.severity = severity;
    }

    public Integer getPointsDeducted() {
        return pointsDeducted;
    }

    public void setPointsDeducted(Integer pointsDeducted) {
        this.pointsDeducted = pointsDeducted;
    }

    public ViolationAction getAction() {
        return action;
    }

    public void setAction(ViolationAction action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
