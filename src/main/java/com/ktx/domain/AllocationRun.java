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

import com.ktx.domain.enums.AllocationRunStatus;

@Entity
@Table(name = "allocation_runs")
public class AllocationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(nullable = false)
    private Boolean dryRun;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AllocationRunStatus status;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime finishedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "run_by", nullable = true)
    private User runBy;

    @Column(columnDefinition = "text")
    private String summaryJson;

    @Column(length = 255)
    private String seedNote;

    @Column(columnDefinition = "text")
    private String weightsJson;

    public AllocationRun() {
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

    public Boolean getDryRun() {
        return dryRun;
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public AllocationRunStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationRunStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public User getRunBy() {
        return runBy;
    }

    public void setRunBy(User runBy) {
        this.runBy = runBy;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getSeedNote() {
        return seedNote;
    }

    public void setSeedNote(String seedNote) {
        this.seedNote = seedNote;
    }

    public String getWeightsJson() {
        return weightsJson;
    }

    public void setWeightsJson(String weightsJson) {
        this.weightsJson = weightsJson;
    }
}
