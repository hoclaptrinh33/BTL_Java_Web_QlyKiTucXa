package com.ktx.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Contract;
import com.ktx.domain.enums.ContractStatus;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Contract c
            WHERE c.bed.room.building.id = :buildingId
              AND c.status IN :statuses
            """)
    boolean existsOccupyingInBuilding(@Param("buildingId") Long buildingId,
            @Param("statuses") Collection<ContractStatus> statuses);
}
