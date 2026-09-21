package com.ktx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;

import com.ktx.domain.Violation;
import com.ktx.domain.enums.ViolationAction;
import com.ktx.domain.enums.ViolationSeverity;
import com.ktx.domain.enums.ViolationType;

public interface ConductService {

    Violation recordViolation(Long studentId,
                             String recordedByUsername,
                             ViolationType type,
                             ViolationSeverity severity,
                             Integer pointsDeducted,
                             ViolationAction action,
                             String description,
                             LocalDateTime occurredAt,
                             Authentication auth);

    List<Violation> getViolationsForStudent(Long studentId);

    List<Violation> getViolationsForStaff(Authentication auth);

    List<Violation> getViolationsForAdmin(Long buildingId);

    void resetAllConductScores();

    void resetConductScoreForStudent(Long studentId);
}
