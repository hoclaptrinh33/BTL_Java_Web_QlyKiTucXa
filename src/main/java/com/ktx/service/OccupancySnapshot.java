package com.ktx.service;

import java.util.Collection;
import java.util.List;

import com.ktx.domain.Bed;
import com.ktx.domain.Room;
import com.ktx.domain.Student;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Building;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.AllocConfig;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.service.impl.OccupancySnapshotImpl;

public interface OccupancySnapshot {

    List<Bed> vacantMatching(Student s, RoomApplication a, AllocConfig c);

    List<Bed> relaxBuildingThenType(Student s, RoomApplication a, AllocConfig c);

    void occupy(Bed b, Student s);

    Collection<Student> occupants(Room r);

    boolean hasVacantBedsMatchingGender(Student s);

    boolean hasVacantBedsMatchingBuilding(Student s, Building b);

    boolean hasVacantBedsMatchingRoomType(Student s, RoomType t);

    static OccupancySnapshot from(BedRepository bedRepo, ContractRepository contractRepo) {
        return new OccupancySnapshotImpl(bedRepo, contractRepo);
    }
}
