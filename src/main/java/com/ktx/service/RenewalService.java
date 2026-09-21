package com.ktx.service;

import java.time.LocalDate;
import java.util.List;

import com.ktx.domain.RenewalRequest;
import com.ktx.domain.enums.RenewalStatus;

public interface RenewalService {

    /**
     * Sinh viên nộp đơn xin gia hạn hợp đồng.
     * Hợp đồng chuyển sang PENDING_RENEWAL.
     */
    RenewalRequest submitRenewal(Long studentId, Integer termMonths, LocalDate requestedEnd, String note);

    /**
     * Admin duyệt đơn gia hạn.
     * Cập nhật end_date = requested_end, hợp đồng chuyển về ACTIVE.
     */
    RenewalRequest approveRenewal(Long requestId, Long adminUserId, String adminNote);

    /**
     * Admin từ chối đơn gia hạn.
     * Hợp đồng PENDING_RENEWAL chuyển về ACTIVE, giữ nguyên end_date cũ.
     */
    RenewalRequest rejectRenewal(Long requestId, Long adminUserId, String adminNote);

    /**
     * Sinh viên chủ động hủy đơn gia hạn.
     * Hợp đồng PENDING_RENEWAL chuyển về ACTIVE, giữ nguyên end_date cũ.
     */
    RenewalRequest cancelRenewal(Long requestId, Long studentId);

    /**
     * Xử lý hợp đồng và đơn gia hạn quá hạn:
     * HĐ PENDING_RENEWAL và end_date < today -> chuyển sang EXPIRED, đơn chuyển sang CANCELLED (admin_note=EXPIRED), giường vẫn chiếm.
     * HĐ ACTIVE và end_date < today -> chuyển sang EXPIRED.
     * Nhắc nhở hết hạn cho các HĐ sắp hết hạn trong 30 ngày.
     */
    int processExpiredContractsAndRenewals(LocalDate today);

    /**
     * Lấy chi tiết đơn kèm thông tin liên quan.
     */
    RenewalRequest getById(Long id);

    /**
     * Lấy danh sách đơn gia hạn của sinh viên.
     */
    List<RenewalRequest> findByStudentId(Long studentId);

    /**
     * Tìm kiếm và lọc đơn gia hạn cho admin.
     */
    List<RenewalRequest> searchRequests(RenewalStatus status, Long buildingId);
}