package com.ktx.service;

import com.ktx.dto.AllocationRunResult;

public interface AllocationEngine {
    AllocationRunResult plan(long periodId);
}
