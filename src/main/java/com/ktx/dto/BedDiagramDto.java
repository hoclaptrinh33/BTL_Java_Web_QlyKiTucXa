package com.ktx.dto;

import com.ktx.domain.enums.BedStatus;

public class BedDiagramDto {
    private Long id;
    private String bedCode;
    private BedStatus status;
    private String statusClass; // "vacant" / "occupied" / "draft"
    private String studentCode;
    private String shortStudentCode;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getShortStudentCode() {
        return shortStudentCode;
    }

    public void setShortStudentCode(String shortStudentCode) {
        this.shortStudentCode = shortStudentCode;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
