package com.ktx.service;

import java.time.LocalDate;

import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomApplication;

public interface ContractService {

    /**
     * Tạo hợp đồng DRAFT từ kết quả phân bổ chỗ ở và khóa giường sang OCCUPIED theo §5.2.6
     */
    Contract createDraftFromAllocation(RoomApplication app, Bed bed, LocalDate termStart, LocalDate termEnd);

    /**
     * Tạo hợp đồng DRAFT cho sinh viên và khóa giường sang OCCUPIED
     */
    Contract createDraft(com.ktx.domain.Student student, Bed bed, RoomApplication app, LocalDate termStart, LocalDate termEnd);

    /**
     * Hủy hợp đồng DRAFT và nhả giường về VACANT
     */
    void cancelDraft(Long contractId);
}
