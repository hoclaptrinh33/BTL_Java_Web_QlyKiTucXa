package com.ktx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_sequences")
@IdClass(DocumentSequenceId.class)
public class DocumentSequence {

    @Id
    @Column(nullable = false, length = 20)
    private String kind;

    @Id
    @Column(name = "`year`", nullable = false)
    private Integer year;

    @Column(name = "`last_value`", nullable = false)
    private Integer lastValue;

    public DocumentSequence() {
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getLastValue() {
        return lastValue;
    }

    public void setLastValue(Integer lastValue) {
        this.lastValue = lastValue;
    }
}
