package com.ktx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.BuildingForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;

@Service
public class BuildingService {

    public static final String DUPLICATE_CODE = "Mã tòa đã được sử dụng";
    public static final String GENDER_LOCKED =
            "Không đổi giới tính tòa khi còn hợp đồng đang giữ giường";
    public static final String NOT_FOUND = "Không tìm thấy tòa";

    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;

    public BuildingService(BuildingRepository buildingRepository, ContractRepository contractRepository) {
        this.buildingRepository = buildingRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<Building> listAll() {
        return buildingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Building getById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean hasOccupyingContracts(Long buildingId) {
        return contractRepository.existsOccupyingInBuilding(buildingId, OccupyingStatuses.OCCUPYING);
    }

    @Transactional
    public Building create(BuildingForm form) {
        String code = normalizeCode(form.getCode());
        if (buildingRepository.existsByCode(code)) {
            throw new DuplicateFieldException("code", DUPLICATE_CODE);
        }
        Building building = new Building();
        apply(building, form, code);
        return buildingRepository.save(building);
    }

    @Transactional
    public Building update(Long id, BuildingForm form) {
        Building building = getById(id);
        String code = normalizeCode(form.getCode());
        if (buildingRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateFieldException("code", DUPLICATE_CODE);
        }
        BuildingGenderPolicy nextGender = form.getGenderPolicy();
        if (building.getGenderPolicy() != nextGender && hasOccupyingContracts(id)) {
            throw new BusinessException(GENDER_LOCKED);
        }
        apply(building, form, code);
        return buildingRepository.save(building);
    }

    private static void apply(Building building, BuildingForm form, String code) {
        building.setCode(code);
        building.setName(form.getName().trim());
        building.setGenderPolicy(form.getGenderPolicy());
        building.setActive(form.isActive());
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
