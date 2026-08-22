package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.RoomForm;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.SystemConfigRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private SystemConfigRepository systemConfigRepository;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, buildingRepository, bedRepository, systemConfigRepository);
    }

    @Test
    void create_makesSixVacantBedsForStandard6() {
        stubCreateBuilding();
        when(roomRepository.existsByBuildingIdAndRoomNumber(1L, "101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(9L);
            return r;
        });
        when(bedRepository.save(any(Bed.class))).thenAnswer(inv -> inv.getArgument(0));

        Room saved = roomService.create(form(RoomType.STANDARD_6, new BigDecimal("1800000")));

        assertEquals(6, saved.getCapacity());
        assertEquals("101", saved.getRoomNumber());
        assertEquals(new BigDecimal("1800000"), saved.getPricePerTerm());
        ArgumentCaptor<Bed> beds = ArgumentCaptor.forClass(Bed.class);
        verify(bedRepository, times(6)).save(beds.capture());
        assertEquals("G1", beds.getAllValues().getFirst().getBedCode());
        assertEquals(BedStatus.VACANT, beds.getAllValues().getFirst().getStatus());
        assertEquals("G6", beds.getAllValues().get(5).getBedCode());
    }

    @Test
    void create_vipUsesConfigCapacity() {
        stubCreateBuilding();
        when(roomRepository.existsByBuildingIdAndRoomNumber(1L, "101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bedRepository.save(any(Bed.class))).thenAnswer(inv -> inv.getArgument(0));
        SystemConfig config = new SystemConfig();
        config.setConfigValue("4");
        when(systemConfigRepository.findById("room.type.vip.capacity")).thenReturn(Optional.of(config));

        Room saved = roomService.create(form(RoomType.VIP_AC, new BigDecimal("4000000")));

        assertEquals(4, saved.getCapacity());
        verify(bedRepository, times(4)).save(any(Bed.class));
    }

    @Test
    void create_rejectsDuplicateNumberInSameBuilding() {
        Building building = new Building();
        building.setId(1L);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(roomRepository.existsByBuildingIdAndRoomNumber(1L, "101")).thenReturn(true);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class,
                () -> roomService.create(form(RoomType.STANDARD_4, new BigDecimal("2400000"))));
        assertEquals("roomNumber", ex.getField());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void update_rejectsMaintenanceWhenOccupied() {
        when(roomRepository.findByIdWithBuilding(9L)).thenReturn(Optional.of(existingRoom()));
        when(roomRepository.existsByBuildingIdAndRoomNumberAndIdNot(1L, "101", 9L)).thenReturn(false);
        when(bedRepository.countByRoomIdAndStatus(9L, BedStatus.OCCUPIED)).thenReturn(1L);

        RoomForm form = form(RoomType.STANDARD_6, new BigDecimal("1800000"));
        form.setStatus(RoomStatus.MAINTENANCE);

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.update(9L, form));
        assertEquals(RoomService.CANNOT_MAINTENANCE, ex.getMessage());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void delete_rejectsWhenOccupied() {
        when(roomRepository.findByIdWithBuilding(9L)).thenReturn(Optional.of(existingRoom()));
        when(bedRepository.countByRoomIdAndStatus(9L, BedStatus.OCCUPIED)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.delete(9L));
        assertEquals(RoomService.CANNOT_DELETE_OCCUPIED, ex.getMessage());
        verify(roomRepository, never()).delete(any());
    }

    @Test
    void delete_removesVacantBedsThenRoom() {
        Room room = existingRoom();
        when(roomRepository.findByIdWithBuilding(9L)).thenReturn(Optional.of(room));
        when(bedRepository.countByRoomIdAndStatus(9L, BedStatus.OCCUPIED)).thenReturn(0L);
        List<Bed> beds = List.of(new Bed(), new Bed());
        when(bedRepository.findByRoomIdOrderByBedCodeAsc(9L)).thenReturn(beds);

        roomService.delete(9L);

        verify(bedRepository).deleteAll(beds);
        verify(roomRepository).delete(room);
    }

    private void stubCreateBuilding() {
        Building building = new Building();
        building.setId(1L);
        building.setCode("A");
        building.setGenderPolicy(BuildingGenderPolicy.MALE);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    }

    private static Room existingRoom() {
        Building building = new Building();
        building.setId(1L);
        building.setCode("A");
        Room room = new Room();
        room.setId(9L);
        room.setBuilding(building);
        room.setRoomNumber("101");
        room.setFloor(1);
        room.setRoomType(RoomType.STANDARD_6);
        room.setCapacity(6);
        room.setPricePerTerm(new BigDecimal("1800000"));
        room.setStatus(RoomStatus.ACTIVE);
        return room;
    }

    private static RoomForm form(RoomType type, BigDecimal price) {
        RoomForm form = new RoomForm();
        form.setBuildingId(1L);
        form.setRoomNumber("101");
        form.setFloor(1);
        form.setRoomType(type);
        form.setPricePerTerm(price);
        form.setStatus(RoomStatus.ACTIVE);
        return form;
    }
}
