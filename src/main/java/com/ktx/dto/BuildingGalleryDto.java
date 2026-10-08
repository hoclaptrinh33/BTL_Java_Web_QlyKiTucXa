package com.ktx.dto;

import java.util.ArrayList;
import java.util.List;

import com.ktx.domain.enums.BuildingGenderPolicy;

public class BuildingGalleryDto {

    private Long id;
    private String code;
    private String name;
    private BuildingGenderPolicy genderPolicy;
    private String genderLabel;
    private String primaryImageUrl;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> captions = new ArrayList<>();
    private int totalImages;
    private int roomCount;
    private int bedCount;
    private int vacantBeds;
    private String address;
    private String description;
    private List<String> amenities = new ArrayList<>();

    public BuildingGalleryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BuildingGenderPolicy getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(BuildingGenderPolicy genderPolicy) {
        this.genderPolicy = genderPolicy;
    }

    public String getGenderLabel() {
        return genderLabel;
    }

    public void setGenderLabel(String genderLabel) {
        this.genderLabel = genderLabel;
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public void setPrimaryImageUrl(String primaryImageUrl) {
        this.primaryImageUrl = primaryImageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getCaptions() {
        return captions;
    }

    public void setCaptions(List<String> captions) {
        this.captions = captions;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public int getVacantBeds() {
        return vacantBeds;
    }

    public void setVacantBeds(int vacantBeds) {
        this.vacantBeds = vacantBeds;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }
}
