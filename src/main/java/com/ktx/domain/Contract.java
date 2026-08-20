package com.ktx.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.ktx.domain.enums.CompletionReason;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String contractNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bed_id", nullable = false)
    private Bed bed;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "application_id", nullable = true)
    private RoomApplication application;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal roomFee;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal depositAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DepositStatus depositStatus;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @Column(length = 40)
    @Enumerated(EnumType.STRING)
    private CompletionReason completionReason;

    @Column(length = 20)
    private String termsVersion;

    @Column
    private LocalDateTime signedAt;

    @Column(insertable = false, updatable = false)
    private Long activeBedKey;

    @Column(insertable = false, updatable = false)
    private Long activeStudentKey;

    public Contract() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Bed getBed() {
        return bed;
    }

    public void setBed(Bed bed) {
        this.bed = bed;
    }

    public RoomApplication getApplication() {
        return application;
    }

    public void setApplication(RoomApplication application) {
        this.application = application;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getRoomFee() {
        return roomFee;
    }

    public void setRoomFee(BigDecimal roomFee) {
        this.roomFee = roomFee;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public DepositStatus getDepositStatus() {
        return depositStatus;
    }

    public void setDepositStatus(DepositStatus depositStatus) {
        this.depositStatus = depositStatus;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public CompletionReason getCompletionReason() {
        return completionReason;
    }

    public void setCompletionReason(CompletionReason completionReason) {
        this.completionReason = completionReason;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public Long getActiveBedKey() {
        return activeBedKey;
    }

    public void setActiveBedKey(Long activeBedKey) {
        this.activeBedKey = activeBedKey;
    }

    public Long getActiveStudentKey() {
        return activeStudentKey;
    }

    public void setActiveStudentKey(Long activeStudentKey) {
        this.activeStudentKey = activeStudentKey;
    }
}
