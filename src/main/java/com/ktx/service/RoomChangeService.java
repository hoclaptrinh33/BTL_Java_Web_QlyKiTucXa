package com.ktx.service;

import java.util.List;
import java.util.Map;

import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;

public interface RoomChangeService {

    /**
     * Sinh viên nộp đơn xin đổi phòng.
     */
    RoomChangeRequest submitRoomChangeRequest(Long studentId, Long requestedBuildingId, RoomType requestedRoomType, String reason);

    /**
     * Sinh viên nộp đơn đăng ký trả phòng.
     */
    RoomChangeRequest submitReturnRoomRequest(Long studentId, String returnDate, String reason, String bankName, String bankAccount);

    /**
     * Sinh viên hủy đơn đang chờ duyệt.
     */
    RoomChangeRequest cancelRequest(Long requestId, Long studentId);

    /**
     * Cán bộ/Admin duyệt và thực hiện chuyển phòng.
     * Quy tắc: Khóa 2 giường ORDER BY id ASC tránh deadlock; cùng transaction nhả giường cũ, chiếm giường mới, cập nhật hợp đồng.
     */
    RoomChangeRequest approveAndExecuteRoomChange(Long requestId, Long targetBedId, Long adminUserId, String adminNote);

    /**
     * Cán bộ/Admin duyệt trả phòng và kích hoạt workflow checkout.
     */
    RoomChangeRequest approveReturnRoom(Long requestId, Long staffUserId, String assetNote, Boolean ok, DepositStatus depositDecision, boolean force, Map<Long, AssetCondition> assetConditions);

    /**
     * Từ chối đơn xin đổi hoặc trả phòng.
     */
    RoomChangeRequest rejectRequest(Long requestId, Long adminUserId, String adminNote);

    /**
     * Lấy chi tiết đơn kèm thông tin liên quan.
     */
    RoomChangeRequest getById(Long id);

    /**
     * Lấy danh sách đơn của một sinh viên theo loại.
     */
    List<RoomChangeRequest> findByStudentIdAndKind(Long studentId, RoomChangeKind kind);

    /**
     * Tìm kiếm và lọc đơn chuyển / trả phòng cho admin / staff.
     */
    List<RoomChangeRequest> searchRequests(RoomChangeKind kind, RoomChangeStatus status, Long buildingId);
}