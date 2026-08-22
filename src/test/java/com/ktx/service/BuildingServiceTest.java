package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.BuildingForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private ContractRepository contractRepository;

    private BuildingService buildingService;

    @BeforeEach
    void setUp() {
        buildingService = new BuildingService(buildingRepository, contractRepository);
    }

    @Test
    void create_normalizesCodeAndSavesMaleBuilding() {
        BuildingForm form = form(" a ", "Tòa A — Nam", BuildingGenderPolicy.MALE, true);
        when(buildingRepository.existsByCode("A")).thenReturn(false);
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));

        Building saved = buildingService.create(form);

        ArgumentCaptor<Building> captor = ArgumentCaptor.forClass(Building.class);
        verify(buildingRepository).save(captor.capture());
        assertEquals("A", captor.getValue().getCode());
        assertEquals("Tòa A — Nam", captor.getValue().getName());
        assertEquals(BuildingGenderPolicy.MALE, captor.getValue().getGenderPolicy());
        assertEquals(Boolean.TRUE, captor.getValue().getActive());
        assertEquals("A", saved.getCode());
    }

    @Test
    void create_duplicateCode_throws() {
        BuildingForm form = form("A", "Tòa A", BuildingGenderPolicy.MALE, true);
        when(buildingRepository.existsByCode("A")).thenReturn(true);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class, () -> buildingService.create(form));
        assertEquals("code", ex.getField());
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void update_rejectsGenderChangeWhenOccupying() {
        Building existing = building(1L, "A", BuildingGenderPolicy.MALE);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(buildingRepository.existsByCodeAndIdNot("A", 1L)).thenReturn(false);
        when(contractRepository.existsOccupyingInBuilding(eq(1L), eq(OccupyingStatuses.OCCUPYING))).thenReturn(true);

        BuildingForm form = form("A", "Tòa A", BuildingGenderPolicy.FEMALE, true);

        BusinessException ex = assertThrows(BusinessException.class, () -> buildingService.update(1L, form));
        assertEquals(BuildingService.GENDER_LOCKED, ex.getMessage());
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void update_allowsGenderChangeWhenEmpty() {
        Building existing = building(1L, "A", BuildingGenderPolicy.MALE);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(buildingRepository.existsByCodeAndIdNot("B", 1L)).thenReturn(false);
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));

        BuildingForm form = form("b", "Tòa B — Nữ", BuildingGenderPolicy.FEMALE, false);
        Building saved = buildingService.update(1L, form);

        assertEquals("B", saved.getCode());
        assertEquals(BuildingGenderPolicy.FEMALE, saved.getGenderPolicy());
        assertEquals(Boolean.FALSE, saved.getActive());
    }

    private static BuildingForm form(String code, String name, BuildingGenderPolicy gender, boolean active) {
        BuildingForm form = new BuildingForm();
        form.setCode(code);
        form.setName(name);
        form.setGenderPolicy(gender);
        form.setActive(active);
        return form;
    }

    private static Building building(Long id, String code, BuildingGenderPolicy gender) {
        Building building = new Building();
        building.setId(id);
        building.setCode(code);
        building.setName("Tòa " + code);
        building.setGenderPolicy(gender);
        building.setActive(true);
        return building;
    }
}
