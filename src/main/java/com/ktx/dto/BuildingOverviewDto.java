package com.ktx.dto;

public class BuildingOverviewDto {

    private long totalBuildings;
    private long totalRooms;
    private long totalBeds;
    private long occupiedBeds;
    private long vacantBeds;
    private long maintenanceBeds;
    private double occupancyPercent;
    private double vacantPercent;

    public BuildingOverviewDto() {
    }

    public long getTotalBuildings() {
        return totalBuildings;
    }

    public void setTotalBuildings(long totalBuildings) {
        this.totalBuildings = totalBuildings;
    }

    public long getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(long totalRooms) {
        this.totalRooms = totalRooms;
    }

    public long getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(long totalBeds) {
        this.totalBeds = totalBeds;
    }

    public long getOccupiedBeds() {
        return occupiedBeds;
    }

    public void setOccupiedBeds(long occupiedBeds) {
        this.occupiedBeds = occupiedBeds;
    }

    public long getVacantBeds() {
        return vacantBeds;
    }

    public void setVacantBeds(long vacantBeds) {
        this.vacantBeds = vacantBeds;
    }

    public long getMaintenanceBeds() {
        return maintenanceBeds;
    }

    public void setMaintenanceBeds(long maintenanceBeds) {
        this.maintenanceBeds = maintenanceBeds;
    }

    public double getOccupancyPercent() {
        return occupancyPercent;
    }

    public void setOccupancyPercent(double occupancyPercent) {
        this.occupancyPercent = occupancyPercent;
    }

    public double getVacantPercent() {
        return vacantPercent;
    }

    public void setVacantPercent(double vacantPercent) {
        this.vacantPercent = vacantPercent;
    }
}
