package com.ktx.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardSnapshot {

    private long studentCount;
    private long occupyingStudentCount;
    private long activeRoomCount;
    private long roomCount;
    private long buildingCount;
    private long occupiedBeds;
    private long vacantBeds;
    private long maintenanceBeds;
    private double occupancyPercent;
    private long emptyRooms;
    private long fullRooms;
    private long maintenanceRooms;
    private int unreadNotifications;
    private Long buildingId;
    private String buildingName;
    private String buildingCode;
    private boolean staffView;
    private long openTicketCount;
    private long expiringContractCount;
    private long overdueInvoiceCount;
    private long openPeriodSubmittedCount;
    private long openPeriodAllocatedCount;
    private long openPeriodWaitlistedCount;
    private String openPeriodName;
    private boolean hasOpenPeriod;
    private DebtByMonthDto debtByMonth;
    private final List<BuildingOccupancy> buildings = new ArrayList<>();
    private final List<RecentApplicationRow> recentApplications = new ArrayList<>();
    private final List<DashboardNotice> notices = new ArrayList<>();

    public long getStudentCount() { return studentCount; }
    public void setStudentCount(long studentCount) { this.studentCount = studentCount; }
    public long getOccupyingStudentCount() { return occupyingStudentCount; }
    public void setOccupyingStudentCount(long occupyingStudentCount) { this.occupyingStudentCount = occupyingStudentCount; }
    public long getActiveRoomCount() { return activeRoomCount; }
    public void setActiveRoomCount(long activeRoomCount) { this.activeRoomCount = activeRoomCount; }
    public long getRoomCount() { return roomCount; }
    public void setRoomCount(long roomCount) { this.roomCount = roomCount; }
    public long getBuildingCount() { return buildingCount; }
    public void setBuildingCount(long buildingCount) { this.buildingCount = buildingCount; }
    public long getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(long occupiedBeds) { this.occupiedBeds = occupiedBeds; }
    public long getVacantBeds() { return vacantBeds; }
    public void setVacantBeds(long vacantBeds) { this.vacantBeds = vacantBeds; }
    public long getMaintenanceBeds() { return maintenanceBeds; }
    public void setMaintenanceBeds(long maintenanceBeds) { this.maintenanceBeds = maintenanceBeds; }
    public long getTotalBeds() { return occupiedBeds + vacantBeds + maintenanceBeds; }
    public long getCapacityBeds() { return occupiedBeds + vacantBeds; }
    public double getOccupancyPercent() { return occupancyPercent; }
    public void setOccupancyPercent(double occupancyPercent) { this.occupancyPercent = occupancyPercent; }
    public String getOccupancyPercentLabel() { return String.format(Locale.US, "%.1f", occupancyPercent); }
    public String getVacantPercentLabel() { return String.format(Locale.US, "%.1f", Math.max(0, 100.0 - occupancyPercent)); }
    public long getEmptyRooms() { return emptyRooms; }
    public void setEmptyRooms(long emptyRooms) { this.emptyRooms = emptyRooms; }
    public long getFullRooms() { return fullRooms; }
    public void setFullRooms(long fullRooms) { this.fullRooms = fullRooms; }
    public long getMaintenanceRooms() { return maintenanceRooms; }
    public void setMaintenanceRooms(long maintenanceRooms) { this.maintenanceRooms = maintenanceRooms; }
    public int getUnreadNotifications() { return unreadNotifications; }
    public void setUnreadNotifications(int unreadNotifications) { this.unreadNotifications = unreadNotifications; }
    public List<BuildingOccupancy> getBuildings() { return buildings; }
    public List<RecentApplicationRow> getRecentApplications() { return recentApplications; }
    public List<DashboardNotice> getNotices() { return notices; }
    public boolean hasBeds() { return getTotalBeds() > 0; }

    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getBuildingCode() { return buildingCode; }
    public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }
    public boolean isStaffView() { return staffView; }
    public void setStaffView(boolean staffView) { this.staffView = staffView; }
    public long getOpenTicketCount() { return openTicketCount; }
    public void setOpenTicketCount(long openTicketCount) { this.openTicketCount = openTicketCount; }
    public long getExpiringContractCount() { return expiringContractCount; }
    public void setExpiringContractCount(long expiringContractCount) { this.expiringContractCount = expiringContractCount; }
    public long getOverdueInvoiceCount() { return overdueInvoiceCount; }
    public void setOverdueInvoiceCount(long overdueInvoiceCount) { this.overdueInvoiceCount = overdueInvoiceCount; }
    public long getOpenPeriodSubmittedCount() { return openPeriodSubmittedCount; }
    public void setOpenPeriodSubmittedCount(long openPeriodSubmittedCount) { this.openPeriodSubmittedCount = openPeriodSubmittedCount; }
    public long getOpenPeriodAllocatedCount() { return openPeriodAllocatedCount; }
    public void setOpenPeriodAllocatedCount(long openPeriodAllocatedCount) { this.openPeriodAllocatedCount = openPeriodAllocatedCount; }
    public long getOpenPeriodWaitlistedCount() { return openPeriodWaitlistedCount; }
    public void setOpenPeriodWaitlistedCount(long openPeriodWaitlistedCount) { this.openPeriodWaitlistedCount = openPeriodWaitlistedCount; }
    public String getOpenPeriodName() { return openPeriodName; }
    public void setOpenPeriodName(String openPeriodName) { this.openPeriodName = openPeriodName; }
    public boolean isHasOpenPeriod() { return hasOpenPeriod; }
    public void setHasOpenPeriod(boolean hasOpenPeriod) { this.hasOpenPeriod = hasOpenPeriod; }
    public DebtByMonthDto getDebtByMonth() { return debtByMonth; }
    public void setDebtByMonth(DebtByMonthDto debtByMonth) { this.debtByMonth = debtByMonth; }

    public static class BuildingOccupancy {
        private String code;
        private String name;
        private String genderLabel;
        private long occupied;
        private long vacant;
        private long maintenance;
        private long rooms;
        private double occupancyPercent;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getGenderLabel() { return genderLabel; }
        public void setGenderLabel(String genderLabel) { this.genderLabel = genderLabel; }
        public long getOccupied() { return occupied; }
        public void setOccupied(long occupied) { this.occupied = occupied; }
        public long getVacant() { return vacant; }
        public void setVacant(long vacant) { this.vacant = vacant; }
        public long getMaintenance() { return maintenance; }
        public void setMaintenance(long maintenance) { this.maintenance = maintenance; }
        public long getRooms() { return rooms; }
        public void setRooms(long rooms) { this.rooms = rooms; }
        public long getTotalBeds() { return occupied + vacant + maintenance; }
        public double getOccupancyPercent() { return occupancyPercent; }
        public void setOccupancyPercent(double occupancyPercent) { this.occupancyPercent = occupancyPercent; }
        public String getOccupancyPercentLabel() { return String.format(Locale.US, "%.0f", occupancyPercent); }
        public double getOccupiedShare() { return share(occupied); }
        public double getVacantShare() { return share(vacant); }
        public double getMaintenanceShare() { return share(maintenance); }
        private double share(long part) {
            long total = getTotalBeds();
            return total == 0 ? 0 : part * 100.0 / total;
        }
    }

    public static class RecentApplicationRow {
        private Long id;
        private String code;
        private String studentName;
        private String studentCode;
        private String initials;
        private String submittedAt;
        private String roomTypeLabel;
        private String priorityLabel;
        private String priorityTone;
        private String statusLabel;
        private String statusTone;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
        public String getInitials() { return initials; }
        public void setInitials(String initials) { this.initials = initials; }
        public String getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
        public String getRoomTypeLabel() { return roomTypeLabel; }
        public void setRoomTypeLabel(String roomTypeLabel) { this.roomTypeLabel = roomTypeLabel; }
        public String getPriorityLabel() { return priorityLabel; }
        public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }
        public String getPriorityTone() { return priorityTone; }
        public void setPriorityTone(String priorityTone) { this.priorityTone = priorityTone; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getStatusTone() { return statusTone; }
        public void setStatusTone(String statusTone) { this.statusTone = statusTone; }
    }

    public static class DashboardNotice {
        private final String tone;
        private final String title;
        private final String body;
        private final String timeLabel;

        public DashboardNotice(String tone, String title, String body, String timeLabel) {
            this.tone = tone;
            this.title = title;
            this.body = body;
            this.timeLabel = timeLabel;
        }

        public String getTone() { return tone; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getTimeLabel() { return timeLabel; }
    }
}
