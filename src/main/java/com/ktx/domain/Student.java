package com.ktx.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.PriorityCategory;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String studentCode;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String facultyCode;

    @Column(length = 30)
    private String classCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String emergencyName;

    @Column(length = 20)
    private String emergencyPhone;

    @Column(length = 120)
    private String hometown;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PriorityCategory priorityCategory;

    @Column(nullable = false)
    private Boolean previousStayGood;

    @Column(nullable = false)
    private Integer conductScore;

    @Column(nullable = false)
    private Boolean blockedFromHousing;

    public Student() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getFacultyCode() {
        return facultyCode;
    }

    public void setFacultyCode(String facultyCode) {
        this.facultyCode = facultyCode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

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

    public PriorityCategory getPriorityCategory() {
        return priorityCategory;
    }

    public void setPriorityCategory(PriorityCategory priorityCategory) {
        this.priorityCategory = priorityCategory;
    }

    public Boolean getPreviousStayGood() {
        return previousStayGood;
    }

    public void setPreviousStayGood(Boolean previousStayGood) {
        this.previousStayGood = previousStayGood;
    }

    public Integer getConductScore() {
        return conductScore;
    }

    public void setConductScore(Integer conductScore) {
        this.conductScore = conductScore;
    }

    public Boolean getBlockedFromHousing() {
        return blockedFromHousing;
    }

    public void setBlockedFromHousing(Boolean blockedFromHousing) {
        this.blockedFromHousing = blockedFromHousing;
    }
}
