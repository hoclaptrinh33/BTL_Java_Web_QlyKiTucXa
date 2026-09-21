package com.ktx.dto;

import jakarta.validation.constraints.NotNull;

public class ManualAssignForm {

    @NotNull(message = "Vui lòng chọn sinh viên")
    private Long studentId;

    @NotNull(message = "Vui lòng chọn giường")
    private Long bedId;

    private Long periodId;

    private String note;

    public ManualAssignForm() {
    }

    public ManualAssignForm(Long studentId, Long bedId, Long periodId, String note) {
        this.studentId = studentId;
        this.bedId = bedId;
        this.periodId = periodId;
        this.note = note;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
