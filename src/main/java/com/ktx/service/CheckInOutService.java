package com.ktx.service;

import java.util.List;
import java.util.Map;

import com.ktx.domain.CheckInOut;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.DepositStatus;

public interface CheckInOutService {

    /**
     * Thực hiện Check-in hợp đồng DRAFT -> ACTIVE.
     * Lập biên bản bàn giao tài sản và phát hành hóa đơn cọc (50% giá kỳ).
     */
    CheckInOut checkIn(Long contractId, Long staffUserId, String assetNote, Boolean ok, Map<Long, AssetCondition> assetConditions);

    /**
     * Thực hiện Check-out hợp đồng (ACTIVE, EXPIRED hoặc TERMINATED) -> COMPLETED.
     * Lập biên bản bàn giao tài sản, xử lý cọc và nhả giường về VACANT.
     */
    CheckInOut checkOut(Long contractId, Long staffUserId, String assetNote, Boolean ok, DepositStatus depositDecision, boolean force, Map<Long, AssetCondition> assetConditions);

    /**
     * Lịch sử check-in / check-out của một hợp đồng
     */
    List<CheckInOut> findByContractId(Long contractId);

    /**
     * Danh sách lịch sử check-in / check-out gần đây (có thể lọc theo tòa)
     */
    List<CheckInOut> findRecent(Long buildingId);
}
