package com.ktx.service;

import java.time.LocalDate;

import java.util.Collection;
import java.util.List;

import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.enums.ContractStatus;

public interface ContractService {

    /**
     * Tạo hợp đồng DRAFT từ kết quả phân bổ chỗ ở và khóa giường sang OCCUPIED theo §5.2.6
     */
    Contract createDraftFromAllocation(RoomApplication app, Bed bed, LocalDate termStart, LocalDate termEnd);

    /**
     * Tạo hợp đồng DRAFT cho sinh viên (dùng cho cả phân bổ tự động và gán tay thủ công)
     */
    Contract createDraft(com.ktx.domain.Student student, RoomApplication app, Bed bed, LocalDate termStart, LocalDate termEnd);

    /**
     * Hủy hợp đồng DRAFT và nhả giường về VACANT
     */
    void cancelDraft(Long contractId);

    /**
     * Lấy thông tin hợp đồng theo ID
     */
    Contract getById(Long id);

    /**
     * Lấy thông tin hợp đồng kèm chi tiết phòng, giường, sinh viên
     */
    Contract getByIdWithDetails(Long id);

    /**
     * Tìm kiếm và lọc danh sách hợp đồng
     */
    List<Contract> searchContracts(Long buildingId, ContractStatus status, String keyword);

    /**
     * Danh sách hợp đồng theo tòa và nhóm trạng thái
     */
    List<Contract> findByBuildingAndStatus(Long buildingId, Collection<ContractStatus> statuses);

    /**
     * Chấm dứt hợp đồng ACTIVE -> TERMINATED (vi phạm kỷ luật hoặc điểm rèn luyện 0).
     * Giường VẪN giữ OCCUPIED cho đến khi checkout (§04-04).
     */
    void terminate(Long contractId, boolean forfeitDeposit);
}
