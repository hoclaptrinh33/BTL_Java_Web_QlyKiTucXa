package com.ktx.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.ktx.domain.enums.PeriodGenderScope;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;

@Entity
@Table(name = "registration_periods")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false)
    private LocalDateTime openAt;

    @Column(nullable = false)
    private LocalDateTime closeAt;

    @Column(nullable = false)
    private LocalDate termStart;

    @Column(nullable = false)
    private LocalDate termEnd;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodGenderScope genderScope = PeriodGenderScope.ALL;

    @Column(name = "min_conduct_score")
    private Integer minConductScore = 0;

    @Column(name = "target_cohort", length = 100)
    private String targetCohort;

    @Column(name = "target_quota")
    private Integer targetQuota;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "registration_period_buildings",
        joinColumns = @JoinColumn(name = "period_id"),
        inverseJoinColumns = @JoinColumn(name = "building_id")
    )
    private Set<Building> buildings = new HashSet<>();

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    @Column(name = "checkin_start")
    private LocalDate checkinStart;

    @Column(name = "checkin_end")
    private LocalDate checkinEnd;

    @Column(name = "deposit_ratio", precision = 4, scale = 2)
    private BigDecimal depositRatio = BigDecimal.valueOf(0.50);

    @Column(name = "payment_guide", columnDefinition = "TEXT")
    private String paymentGuide;

    @Column(name = "require_document_proof", nullable = false)
    private Boolean requireDocumentProof = false;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    public RegistrationPeriod() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDateTime getOpenAt() {
        return openAt;
    }

    public void setOpenAt(LocalDateTime openAt) {
        this.openAt = openAt;
    }

    public LocalDateTime getCloseAt() {
        return closeAt;
    }

    public void setCloseAt(LocalDateTime closeAt) {
        this.closeAt = closeAt;
    }

    public LocalDate getTermStart() {
        return termStart;
    }

    public void setTermStart(LocalDate termStart) {
        this.termStart = termStart;
    }

    public LocalDate getTermEnd() {
        return termEnd;
    }

    public void setTermEnd(LocalDate termEnd) {
        this.termEnd = termEnd;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public PeriodGenderScope getGenderScope() {
        return genderScope;
    }

    public void setGenderScope(PeriodGenderScope genderScope) {
        this.genderScope = genderScope;
    }

    public Integer getMinConductScore() {
        return minConductScore;
    }

    public void setMinConductScore(Integer minConductScore) {
        this.minConductScore = minConductScore;
    }

    public String getTargetCohort() {
        return targetCohort;
    }

    public void setTargetCohort(String targetCohort) {
        this.targetCohort = targetCohort;
    }

    public Integer getTargetQuota() {
        return targetQuota;
    }

    public void setTargetQuota(Integer targetQuota) {
        this.targetQuota = targetQuota;
    }

    public Set<Building> getBuildings() {
        return buildings;
    }

    public void setBuildings(Set<Building> buildings) {
        this.buildings = buildings;
    }

    public LocalDateTime getPaymentDeadline() {
        return paymentDeadline;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public LocalDate getCheckinStart() {
        return checkinStart;
    }

    public void setCheckinStart(LocalDate checkinStart) {
        this.checkinStart = checkinStart;
    }

    public LocalDate getCheckinEnd() {
        return checkinEnd;
    }

    public void setCheckinEnd(LocalDate checkinEnd) {
        this.checkinEnd = checkinEnd;
    }

    public BigDecimal getDepositRatio() {
        return depositRatio;
    }

    public void setDepositRatio(BigDecimal depositRatio) {
        this.depositRatio = depositRatio;
    }

    public String getPaymentGuide() {
        return paymentGuide;
    }

    public void setPaymentGuide(String paymentGuide) {
        this.paymentGuide = paymentGuide;
    }

    public Boolean getRequireDocumentProof() {
        return requireDocumentProof;
    }

    public void setRequireDocumentProof(Boolean requireDocumentProof) {
        this.requireDocumentProof = requireDocumentProof;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
