package com.ktx.dto;

import com.ktx.domain.enums.BuildingGenderPolicy;

public class BuildingRowDto {

    private Long id;
    private String code;
    private String name;
    private BuildingGenderPolicy genderPolicy;
    private String genderLabel;
    private String zone;
    private String zoneClass;
    private boolean active;
    private String statusLabel;
    private String statusClass;
    private int floorCount;
    private int roomCount;
    private int bedCount;
    private int occupiedBeds;
    private int vacantBeds;
    private int maintenanceBeds;
    private double occupancyPercent;
    private String address;
    private String imageUrl;

    public BuildingRowDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BuildingGenderPolicy getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(BuildingGenderPolicy genderPolicy) {
        this.genderPolicy = genderPolicy;
    }

    public String getGenderLabel() {
        return genderLabel;
    }

    public void setGenderLabel(String genderLabel) {
        this.genderLabel = genderLabel;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getZoneClass() {
        return zoneClass;
    }

    public void setZoneClass(String zoneClass) {
        this.zoneClass = zoneClass;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(int floorCount) {
        this.floorCount = floorCount;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public int getOccupiedBeds() {
        return occupiedBeds;
    }

    public void setOccupiedBeds(int occupiedBeds) {
        this.occupiedBeds = occupiedBeds;
    }

    public int getVacantBeds() {
        return vacantBeds;
    }

    public void setVacantBeds(int vacantBeds) {
        this.vacantBeds = vacantBeds;
    }

    public int getMaintenanceBeds() {
        return maintenanceBeds;
    }

    public void setMaintenanceBeds(int maintenanceBeds) {
        this.maintenanceBeds = maintenanceBeds;
    }

    public double getOccupancyPercent() {
        return occupancyPercent;
    }

    public void setOccupancyPercent(double occupancyPercent) {
        this.occupancyPercent = occupancyPercent;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
