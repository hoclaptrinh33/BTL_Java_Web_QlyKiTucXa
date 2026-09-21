package com.ktx.dto;

public class UtilityReadingForm {

    private Long roomId;
    private String billingMonth; // Format "YYYY-MM"

    private Integer elecPrev;
    private Integer elecCurr;
    private Boolean elecReplaced = false;
    private Integer elecOldFinal;
    private Integer elecNewStart;

    private Integer waterPrev;
    private Integer waterCurr;
    private Boolean waterReplaced = false;
    private Integer waterOldFinal;
    private Integer waterNewStart;

    private Boolean newBuildingMeter = false;

    public UtilityReadingForm() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
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

    public Boolean getElecReplaced() {
        return elecReplaced;
    }

    public void setElecReplaced(Boolean elecReplaced) {
        this.elecReplaced = elecReplaced;
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

    public Boolean getWaterReplaced() {
        return waterReplaced;
    }

    public void setWaterReplaced(Boolean waterReplaced) {
        this.waterReplaced = waterReplaced;
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
}
