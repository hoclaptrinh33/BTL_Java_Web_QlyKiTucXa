package com.ktx.domain;

import java.time.LocalDateTime;

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

import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.RoomType;

@Entity
@Table(name = "room_applications")
public class RoomApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "preferred_building_id", nullable = true)
    private Building preferredBuilding;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private RoomType preferredRoomType;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PriorityCategory prioritySnapshot;

    @Column(nullable = false)
    private Boolean previousStayGoodSnapshot;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column
    private LocalDateTime submittedAt;

    @Column
    private Integer computedScore;

    @Column(length = 500)
    private String note;

    public RoomApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationPeriod getPeriod() {
        return period;
    }

    public void setPeriod(RegistrationPeriod period) {
        this.period = period;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Building getPreferredBuilding() {
        return preferredBuilding;
    }

    public void setPreferredBuilding(Building preferredBuilding) {
        this.preferredBuilding = preferredBuilding;
    }

    public RoomType getPreferredRoomType() {
        return preferredRoomType;
    }

    public void setPreferredRoomType(RoomType preferredRoomType) {
        this.preferredRoomType = preferredRoomType;
    }

    public PriorityCategory getPrioritySnapshot() {
        return prioritySnapshot;
    }

    public void setPrioritySnapshot(PriorityCategory prioritySnapshot) {
        this.prioritySnapshot = prioritySnapshot;
    }

    public Boolean getPreviousStayGoodSnapshot() {
        return previousStayGoodSnapshot;
    }

    public void setPreviousStayGoodSnapshot(Boolean previousStayGoodSnapshot) {
        this.previousStayGoodSnapshot = previousStayGoodSnapshot;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getComputedScore() {
        return computedScore;
    }

    public void setComputedScore(Integer computedScore) {
        this.computedScore = computedScore;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
