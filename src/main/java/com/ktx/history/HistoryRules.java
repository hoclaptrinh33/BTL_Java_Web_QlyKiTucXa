package com.ktx.history;

import java.time.LocalDate;

/**
 * Biểu giá và quy tắc mô phỏng 5 năm. Cùng seed thì cùng kết quả.
 * Giá điện là bậc thang sinh hoạt EVN, chưa VAT. Giá phòng và phí phụ
 * chụp vào hợp đồng / hóa đơn; {@code system_configs} giữ giá đang thu.
 */
public final class HistoryRules {

    public static final int SEED = 42;

    public enum Outcome {
        NORMAL,
        CANCEL_BEFORE,
        DROPOUT,
        FORCED,
        ROOM_CHANGE
    }

    /** STANDARD_4, STANDARD_6, STANDARD_8, VIP_AC. */
    public static final String[] ROOM_TYPES = {"STANDARD_4", "STANDARD_6", "STANDARD_8", "VIP_AC"};
    public static final int[] CAPACITY = {4, 6, 8, 2};

    private static final int[] BOUNDS = {50, 100, 200, 300, 400, Integer.MAX_VALUE};

    private static final LocalDate D_2023_05_04 = LocalDate.of(2023, 5, 4);
    private static final LocalDate D_2023_11_09 = LocalDate.of(2023, 11, 9);
    private static final LocalDate D_2024_10_11 = LocalDate.of(2024, 10, 11);
    private static final LocalDate D_2025_05_10 = LocalDate.of(2025, 5, 10);
    private static final LocalDate D_2024_01_01 = LocalDate.of(2024, 1, 1);

    private static final int[] ELEC_2019 = {1678, 1734, 2014, 2536, 2834, 2927};
    private static final int[] ELEC_2023_05 = {1728, 1786, 2074, 2612, 2919, 3015};
    private static final int[] ELEC_2023_11 = {1806, 1866, 2167, 2729, 3050, 3151};
    private static final int[] ELEC_2024_10 = {1893, 1956, 2271, 2860, 3197, 3302};
    private static final int[] ELEC_2025_05 = {1984, 2050, 2380, 2998, 3350, 3460};

    private HistoryRules() {
    }

    public static int mix(long a, long b) {
        long x = a * 0x9E3779B97F4A7C15L ^ (b + 0xBF58476D1CE4E5B9L + SEED);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x = x ^ (x >>> 31);
        return (int) (x & 0x7fffffff);
    }

    /** Trong nhóm đã được giường: phần lớn ở hết kỳ, mỗi tình huống biên chiếm một khoảng cố định. */
    public static Outcome outcome(long studentKey, int academicYear) {
        int r = mix(studentKey, academicYear) % 100;
        if (r < 86) {
            return Outcome.NORMAL;
        }
        if (r < 90) {
            return Outcome.CANCEL_BEFORE;
        }
        if (r < 94) {
            return Outcome.DROPOUT;
        }
        if (r < 96) {
            return Outcome.FORCED;
        }
        return Outcome.ROOM_CHANGE;
    }

    /** Còn trong 4 năm chương trình tại thời điểm đầu năm học {@code academicYear}. */
    public static boolean enrolledAtStart(int entryYear, int academicYear) {
        return academicYear >= entryYear && academicYear <= entryYear + 3;
    }

    public static int[] electricity(LocalDate day) {
        if (day.isBefore(D_2023_05_04)) {
            return ELEC_2019;
        }
        if (day.isBefore(D_2023_11_09)) {
            return ELEC_2023_05;
        }
        if (day.isBefore(D_2024_10_11)) {
            return ELEC_2023_11;
        }
        if (day.isBefore(D_2025_05_10)) {
            return ELEC_2024_10;
        }
        return ELEC_2025_05;
    }

    public static int waterPerM3(LocalDate day) {
        return day.isBefore(D_2024_01_01) ? 9900 : 13500;
    }

    /** Giá một kỳ 5 tháng theo năm bắt đầu năm học và loại phòng 0..3. */
    public static long roomFee(int academicYear, int roomType) {
        long[] row;
        if (academicYear < 2023) {
            row = new long[] {1_800_000L, 1_350_000L, 900_000L, 3_000_000L};
        } else if (academicYear < 2025) {
            row = new long[] {2_100_000L, 1_575_000L, 1_050_000L, 3_500_000L};
        } else {
            row = new long[] {2_400_000L, 1_800_000L, 1_200_000L, 4_000_000L};
        }
        return row[roomType];
    }

    public static long sanitation(int academicYear) {
        if (academicYear < 2023) {
            return 15_000L;
        }
        return academicYear < 2025 ? 18_000L : 20_000L;
    }

    public static long internetPerRoom(int academicYear) {
        if (academicYear < 2023) {
            return 40_000L;
        }
        return academicYear < 2025 ? 45_000L : 50_000L;
    }

    public static long parking(int academicYear) {
        if (academicYear < 2023) {
            return 20_000L;
        }
        return academicYear < 2025 ? 25_000L : 30_000L;
    }

    /** Tiền điện cả phòng theo bậc của ngày ghi chỉ số. */
    public static long tieredCost(int kwh, LocalDate day) {
        int[] price = electricity(day);
        int prev = 0;
        long cost = 0;
        int remain = Math.max(kwh, 0);
        for (int i = 0; i < BOUNDS.length && remain > 0; i++) {
            int span = Math.min(remain, BOUNDS[i] - prev);
            cost += (long) span * price[i];
            remain -= span;
            prev = BOUNDS[i];
        }
        return cost;
    }

    /** kWh từng bậc, cùng thứ tự bậc 1..6. Bậc không dùng thì 0. */
    public static int[] tierUnits(int kwh) {
        int[] units = new int[BOUNDS.length];
        int prev = 0;
        int remain = Math.max(kwh, 0);
        for (int i = 0; i < BOUNDS.length && remain > 0; i++) {
            int span = Math.min(remain, BOUNDS[i] - prev);
            units[i] = span;
            remain -= span;
            prev = BOUNDS[i];
        }
        return units;
    }

    public static long deposit(long roomFee, int ratioMillis) {
        return (roomFee * ratioMillis + 500) / 1000;
    }

    public static int roomTypeIndex(int sequence) {
        int m = Math.floorMod(sequence, 10);
        if (m < 4) {
            return 2;
        }
        if (m < 7) {
            return 1;
        }
        if (m < 9) {
            return 0;
        }
        return 3;
    }

    public static long share(long total, int parts, int index) {
        if (parts <= 0) {
            return 0;
        }
        long base = total / parts;
        long rem = total % parts;
        return base + (index < rem ? 1 : 0);
    }
}
