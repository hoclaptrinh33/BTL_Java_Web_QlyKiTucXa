package com.ktx.dto;

public class AuditLogSummaryDto {

    private long todayTotal;
    private long todayLogins;
    private long todayFailures;
    private long todayConfigChanges;

    public AuditLogSummaryDto() {
    }

    public AuditLogSummaryDto(long todayTotal, long todayLogins, long todayFailures, long todayConfigChanges) {
        this.todayTotal = todayTotal;
        this.todayLogins = todayLogins;
        this.todayFailures = todayFailures;
        this.todayConfigChanges = todayConfigChanges;
    }

    public long getTodayTotal() {
        return todayTotal;
    }

    public void setTodayTotal(long todayTotal) {
        this.todayTotal = todayTotal;
    }

    public long getTodayLogins() {
        return todayLogins;
    }

    public void setTodayLogins(long todayLogins) {
        this.todayLogins = todayLogins;
    }

    public long getTodayFailures() {
        return todayFailures;
    }

    public void setTodayFailures(long todayFailures) {
        this.todayFailures = todayFailures;
    }

    public long getTodayConfigChanges() {
        return todayConfigChanges;
    }

    public void setTodayConfigChanges(long todayConfigChanges) {
        this.todayConfigChanges = todayConfigChanges;
    }
}
