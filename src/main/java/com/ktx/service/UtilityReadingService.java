package com.ktx.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;

import com.ktx.domain.UtilityReading;
import com.ktx.dto.UtilityReadingForm;

public interface UtilityReadingService {

    UtilityReading recordReading(UtilityReadingForm form, Authentication auth);

    UtilityReadingForm prepareForm(Long roomId, YearMonth month);

    Optional<UtilityReading> getReading(Long roomId, YearMonth month);

    List<UtilityReading> getReadingsByBuilding(Long buildingId, YearMonth month);

    List<UtilityReading> getAllReadingsByMonth(YearMonth month);
}
