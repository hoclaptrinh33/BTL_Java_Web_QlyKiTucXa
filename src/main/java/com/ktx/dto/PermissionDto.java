package com.ktx.dto;

public class PermissionDto {
    private String code;
    private String name;
    private String category;
    private boolean system;

    public PermissionDto() {
    }

    public PermissionDto(String code, String name, String category, boolean system) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.system = system;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
