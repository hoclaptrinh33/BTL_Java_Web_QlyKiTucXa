package com.ktx.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "utility_readings")
public class UtilityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate billingMonth;

    @Column(nullable = false)
    private Integer elecPrev;

    @Column(nullable = false)
    private Integer elecCurr;

    @Column(nullable = false)
    private Integer waterPrev;

    @Column(nullable = false)
    private Integer waterCurr;

    @Column(nullable = false)
    private Boolean elecReplaced;

    @Column(nullable = false)
    private Boolean waterReplaced;

    @Column
    private Integer elecOldFinal;

    @Column
    private Integer elecNewStart;

    @Column
    private Integer waterOldFinal;

    @Column
    private Integer waterNewStart;

    @Column(nullable = false)
    private Boolean newBuildingMeter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public UtilityReading() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public Integer getElecPrev() {
        return elecPrev;
    }

    public void setElecPrev(Integer elecPrev) {
        this.elecPrev = elecPrev;
    }

    public Integer getElecCurr() {
        return elecCurr;
    }

    public void setElecCurr(Integer elecCurr) {
        this.elecCurr = elecCurr;
    }

    public Integer getWaterPrev() {
        return waterPrev;
    }

    public void setWaterPrev(Integer waterPrev) {
        this.waterPrev = waterPrev;
    }

    public Integer getWaterCurr() {
        return waterCurr;
    }

    public void setWaterCurr(Integer waterCurr) {
        this.waterCurr = waterCurr;
    }

    public Boolean getElecReplaced() {
        return elecReplaced;
    }

    public void setElecReplaced(Boolean elecReplaced) {
        this.elecReplaced = elecReplaced;
    }

    public Boolean getWaterReplaced() {
        return waterReplaced;
    }

    public void setWaterReplaced(Boolean waterReplaced) {
        this.waterReplaced = waterReplaced;
    }

    public Integer getElecOldFinal() {
        return elecOldFinal;
    }

    public void setElecOldFinal(Integer elecOldFinal) {
        this.elecOldFinal = elecOldFinal;
    }

    public Integer getElecNewStart() {
        return elecNewStart;
    }

    public void setElecNewStart(Integer elecNewStart) {
        this.elecNewStart = elecNewStart;
    }

    public Integer getWaterOldFinal() {
        return waterOldFinal;
    }

    public void setWaterOldFinal(Integer waterOldFinal) {
        this.waterOldFinal = waterOldFinal;
    }

    public Integer getWaterNewStart() {
        return waterNewStart;
    }

    public void setWaterNewStart(Integer waterNewStart) {
        this.waterNewStart = waterNewStart;
    }

    public Boolean getNewBuildingMeter() {
        return newBuildingMeter;
    }

    public void setNewBuildingMeter(Boolean newBuildingMeter) {
        this.newBuildingMeter = newBuildingMeter;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
