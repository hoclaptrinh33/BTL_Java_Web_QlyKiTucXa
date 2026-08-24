package com.ktx.dto;

import java.util.List;
import com.ktx.domain.AllocationItem;

public class AllocationRunResult {
    private final List<AllocationItem> items;

    public AllocationRunResult(List<AllocationItem> items) {
        this.items = items;
    }

    public List<AllocationItem> getItems() {
        return items;
    }
}
