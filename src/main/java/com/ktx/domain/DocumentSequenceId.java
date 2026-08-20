package com.ktx.domain;

import java.io.Serializable;
import java.util.Objects;

public class DocumentSequenceId implements Serializable {

    private String kind;
    private Integer year;

    public DocumentSequenceId() {
    }

    public DocumentSequenceId(String kind, Integer year) {
        this.kind = kind;
        this.year = year;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentSequenceId that)) {
            return false;
        }
        return Objects.equals(kind, that.kind) && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, year);
    }
}
