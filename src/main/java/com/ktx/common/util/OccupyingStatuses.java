package com.ktx.common.util;

import static com.ktx.domain.enums.ContractStatus.ACTIVE;
import static com.ktx.domain.enums.ContractStatus.COMPLETED;
import static com.ktx.domain.enums.ContractStatus.DRAFT;
import static com.ktx.domain.enums.ContractStatus.EXPIRED;
import static com.ktx.domain.enums.ContractStatus.PENDING_RENEWAL;
import static com.ktx.domain.enums.ContractStatus.TERMINATED;

import java.util.EnumSet;
import java.util.Set;

import com.ktx.domain.enums.ContractStatus;

public final class OccupyingStatuses {
    public static final Set<ContractStatus> OCCUPYING = EnumSet.of(
        DRAFT, ACTIVE, PENDING_RENEWAL, EXPIRED, TERMINATED);
    public static final Set<ContractStatus> FREE = EnumSet.of(COMPLETED);
    public static boolean occupies(ContractStatus s) { return OCCUPYING.contains(s); }
    private OccupyingStatuses() {}
}
