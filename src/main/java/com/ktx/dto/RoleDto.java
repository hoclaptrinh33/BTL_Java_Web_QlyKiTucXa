package com.ktx.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RoleDto {

    @NotBlank(message = "Mã vai trò không được để trống")
    @Size(min = 2, max = 50, message = "Mã vai trò từ 2 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã vai trò chỉ gồm chữ in hoa, số và dấu gạch dưới")
    private String code;

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 100, message = "Tên vai trò tối đa 100 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    private boolean systemLocked;

    private String scope = "ALL"; // ALL or BUILDINGS

    private Set<String> permissions = new HashSet<>();

    private int userCount;

    public RoleDto() {
    }

    public RoleDto(String code, String name, String description, boolean systemLocked, String scope, Set<String> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemLocked = systemLocked;
        this.scope = scope;
        if (permissions != null) {
            this.permissions = new HashSet<>(permissions);
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemLocked() {
        return systemLocked;
    }

    public void setSystemLocked(boolean systemLocked) {
        this.systemLocked = systemLocked;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}
