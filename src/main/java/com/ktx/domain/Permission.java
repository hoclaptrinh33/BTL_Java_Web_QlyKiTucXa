package com.ktx.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.ktx.domain.enums.PermissionPlane;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionPlane plane;

    public Permission() {
    }

    public Permission(String code, PermissionPlane plane) {
        this.code = code;
        this.plane = plane;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PermissionPlane getPlane() {
        return plane;
    }

    public void setPlane(PermissionPlane plane) {
        this.plane = plane;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "code='" + code + '\'' +
                ", plane=" + plane +
                '}';
    }
}
