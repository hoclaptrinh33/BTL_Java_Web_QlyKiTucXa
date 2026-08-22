package com.ktx.dto;

import java.util.ArrayList;
import java.util.List;

public class RoomBatchResult {

    private Long buildingId;
    private String buildingCode;
    private int created;
    private int skipped;
    private int bedsCreated;
    private final List<String> skippedNumbers = new ArrayList<>();
    private String firstDoorCode;
    private String lastDoorCode;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getBedsCreated() {
        return bedsCreated;
    }

    public void setBedsCreated(int bedsCreated) {
        this.bedsCreated = bedsCreated;
    }

    public List<String> getSkippedNumbers() {
        return skippedNumbers;
    }

    public String getFirstDoorCode() {
        return firstDoorCode;
    }

    public void setFirstDoorCode(String firstDoorCode) {
        this.firstDoorCode = firstDoorCode;
    }

    public String getLastDoorCode() {
        return lastDoorCode;
    }

    public void setLastDoorCode(String lastDoorCode) {
        this.lastDoorCode = lastDoorCode;
    }
}
