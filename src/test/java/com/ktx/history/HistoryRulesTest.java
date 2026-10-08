package com.ktx.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.ktx.history.HistoryRules.Outcome;

class HistoryRulesTest {

    @Test
    void electricityFollowsDecisionDates() {
        assertEquals(1678, HistoryRules.electricity(LocalDate.of(2023, 5, 3))[0]);
        assertEquals(1728, HistoryRules.electricity(LocalDate.of(2023, 5, 4))[0]);
        assertEquals(1728, HistoryRules.electricity(LocalDate.of(2023, 11, 8))[0]);
        assertEquals(1806, HistoryRules.electricity(LocalDate.of(2023, 11, 9))[0]);
        assertEquals(1806, HistoryRules.electricity(LocalDate.of(2024, 10, 10))[0]);
        assertEquals(1893, HistoryRules.electricity(LocalDate.of(2024, 10, 11))[0]);
        assertEquals(1893, HistoryRules.electricity(LocalDate.of(2025, 5, 9))[0]);
        assertEquals(1984, HistoryRules.electricity(LocalDate.of(2025, 5, 10))[0]);
        assertEquals(3460, HistoryRules.electricity(LocalDate.of(2026, 9, 1))[5]);
    }

    @Test
    void waterStepsOnFirstOf2024() {
        assertEquals(9900, HistoryRules.waterPerM3(LocalDate.of(2023, 12, 1)));
        assertEquals(13500, HistoryRules.waterPerM3(LocalDate.of(2024, 1, 1)));
    }

    @Test
    void roomFeeStepsByAcademicYear() {
        assertEquals(1_800_000L, HistoryRules.roomFee(2022, 0));
        assertEquals(1_575_000L, HistoryRules.roomFee(2024, 1));
        assertEquals(4_000_000L, HistoryRules.roomFee(2026, 3));
    }

    @Test
    void tieredCostSplitsAtBoundaries() {
        LocalDate day = LocalDate.of(2022, 1, 15);
        assertEquals(50L * 1678, HistoryRules.tieredCost(50, day));
        assertEquals(50L * 1678 + 1734, HistoryRules.tieredCost(51, day));
        assertEquals(6, HistoryRules.tierUnits(401).length);
        assertEquals(1, HistoryRules.tierUnits(401)[5]);
    }

    @Test
    void depositRoundsHalfUp() {
        assertEquals(1_200_000L, HistoryRules.deposit(2_400_000L, 500));
        assertEquals(960_000L, HistoryRules.deposit(2_400_000L, 400));
    }

    @Test
    void enrolledWindowIsFourAcademicYears() {
        assertTrue(HistoryRules.enrolledAtStart(2021, 2021));
        assertTrue(HistoryRules.enrolledAtStart(2021, 2024));
        assertFalse(HistoryRules.enrolledAtStart(2021, 2025));
    }

    @Test
    void outcomeBucketsAllAppear() {
        EnumSet<Outcome> seen = EnumSet.noneOf(Outcome.class);
        for (int i = 1; i <= 400; i++) {
            seen.add(HistoryRules.outcome(i, 2023));
        }
        assertEquals(EnumSet.allOf(Outcome.class), seen);
    }
}
