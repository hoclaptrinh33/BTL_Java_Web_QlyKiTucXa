package com.ktx.dto;

public class OccupancyDriftRow {

    private Long bedId;
    private String buildingCode;
    private String roomNumber;
    private String bedCode;
    private String actualStatus;
    private Long actualContractId;
    private String expectedStatus;
    private Long expectedContractId;
    private String expectedContractNo;
    private String studentName;
    private String studentCode;

    public OccupancyDriftRow() {
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getBedCode() {
        return bedCode;
    }

    public void setBedCode(String bedCode) {
        this.bedCode = bedCode;
    }

    public String getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(String actualStatus) {
        this.actualStatus = actualStatus;
    }

    public Long getActualContractId() {
        return actualContractId;
    }

    public void setActualContractId(Long actualContractId) {
        this.actualContractId = actualContractId;
    }

    public String getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(String expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Long getExpectedContractId() {
        return expectedContractId;
    }

    public void setExpectedContractId(Long expectedContractId) {
        this.expectedContractId = expectedContractId;
    }

    public String getExpectedContractNo() {
        return expectedContractNo;
    }

    public void setExpectedContractNo(String expectedContractNo) {
        this.expectedContractNo = expectedContractNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }
}
