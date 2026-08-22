package com.ktx.dto;

public class StudentRow {

    private Long id;
    private String studentCode;
    private String fullName;
    private String initials;
    private String genderLabel;
    private String facultyCode;
    private String classCode;
    private String phone;
    private String priorityLabel;
    private String priorityTone;
    private int conductScore;
    private boolean occupying;
    private boolean blocked;
    private boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getGenderLabel() { return genderLabel; }
    public void setGenderLabel(String genderLabel) { this.genderLabel = genderLabel; }
    public String getFacultyCode() { return facultyCode; }
    public void setFacultyCode(String facultyCode) { this.facultyCode = facultyCode; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }
    public String getPriorityTone() { return priorityTone; }
    public void setPriorityTone(String priorityTone) { this.priorityTone = priorityTone; }
    public int getConductScore() { return conductScore; }
    public void setConductScore(int conductScore) { this.conductScore = conductScore; }
    public boolean isOccupying() { return occupying; }
    public void setOccupying(boolean occupying) { this.occupying = occupying; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getStayLabel() {
        if (occupying) {
            return "Đang ở";
        }
        if (blocked) {
            return "Cấm đăng ký";
        }
        return "Chưa có chỗ";
    }
    public String getStayTone() {
        if (occupying) {
            return "mint";
        }
        if (blocked) {
            return "rose";
        }
        return "muted";
    }
}
