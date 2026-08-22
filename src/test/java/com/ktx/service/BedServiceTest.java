package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.enums.BedStatus;
import com.ktx.repository.BedRepository;

@ExtendWith(MockitoExtension.class)
class BedServiceTest {

    @Mock
    private BedRepository bedRepository;

    private BedService bedService;

    @BeforeEach
    void setUp() {
        bedService = new BedService(bedRepository);
    }

    @Test
    void updateStatus_vacantToMaintenance() {
        Bed bed = vacant(1L);
        when(bedRepository.findByIdAndRoomId(5L, 9L)).thenReturn(Optional.of(bed));
        when(bedRepository.saveAndFlush(bed)).thenReturn(bed);

        Bed updated = bedService.updateStatus(9L, 5L, BedStatus.MAINTENANCE, 1L);

        assertEquals(BedStatus.MAINTENANCE, updated.getStatus());
        verify(bedRepository).saveAndFlush(bed);
    }

    @Test
    void updateStatus_maintenanceToVacant() {
        Bed bed = vacant(1L);
        bed.setStatus(BedStatus.MAINTENANCE);
        when(bedRepository.findByIdAndRoomId(5L, 9L)).thenReturn(Optional.of(bed));
        when(bedRepository.saveAndFlush(bed)).thenReturn(bed);

        Bed updated = bedService.updateStatus(9L, 5L, BedStatus.VACANT, 1L);

        assertEquals(BedStatus.VACANT, updated.getStatus());
    }

    @Test
    void updateStatus_rejectsOccupied() {
        Bed bed = vacant(0L);
        bed.setStatus(BedStatus.OCCUPIED);
        when(bedRepository.findByIdAndRoomId(5L, 9L)).thenReturn(Optional.of(bed));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bedService.updateStatus(9L, 5L, BedStatus.MAINTENANCE, 0L));
        assertEquals(BedService.CANNOT_CHANGE_OCCUPIED, ex.getMessage());
        verify(bedRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStatus_rejectsStaleVersion() {
        when(bedRepository.findByIdAndRoomId(5L, 9L)).thenReturn(Optional.of(vacant(4L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bedService.updateStatus(9L, 5L, BedStatus.MAINTENANCE, 1L));
        assertEquals(BedService.STALE, ex.getMessage());
        verify(bedRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStatus_rejectsSettingOccupied() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bedService.updateStatus(9L, 5L, BedStatus.OCCUPIED, 0L));
        assertEquals(BedService.INVALID_STATUS, ex.getMessage());
        verify(bedRepository, never()).findByIdAndRoomId(any(), any());
    }

    private static Bed vacant(long version) {
        Bed bed = new Bed();
        bed.setId(5L);
        bed.setBedCode("G1");
        bed.setStatus(BedStatus.VACANT);
        bed.setVersion(version);
        return bed;
    }
}
