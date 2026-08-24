package com.ktx.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    boolean existsByStatusAndPeriodType(PeriodStatus status, PeriodType periodType);
    boolean existsByStatusAndPeriodTypeAndIdNot(PeriodStatus status, PeriodType periodType, Long id);

    @Query("SELECT p FROM RegistrationPeriod p JOIN FETCH p.createdBy")
    List<RegistrationPeriod> findAllWithCreator();
}
