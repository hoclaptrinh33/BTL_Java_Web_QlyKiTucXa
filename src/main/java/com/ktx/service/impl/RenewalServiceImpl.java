package com.ktx.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Notification;
import com.ktx.domain.RenewalRequest;
import com.ktx.domain.Student;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.NotificationType;
import com.ktx.domain.enums.RenewalStatus;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RenewalRequestRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.RenewalService;

@Service
public class RenewalServiceImpl implements RenewalService {

    private final RenewalRequestRepository renewalRequestRepository;
    private final ContractRepository contractRepository;
    private final StudentRepository studentRepository;
    private final NotificationRepository notificationRepository;

    public RenewalServiceImpl(RenewalRequestRepository renewalRequestRepository,
                              ContractRepository contractRepository,
                              StudentRepository studentRepository,
                              NotificationRepository notificationRepository) {
        this.renewalRequestRepository = renewalRequestRepository;
        this.contractRepository = contractRepository;
        this.studentRepository = studentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public RenewalRequest submitRenewal(Long studentId, Integer termMonths, LocalDate requestedEnd, String note) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên #" + studentId));

        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                studentId, OccupyingStatuses.OCCUPYING);
        if (contracts.isEmpty()) {
            throw new BusinessException("Bạn chưa có hợp đồng phòng ở để xin gia hạn");
        }
        Contract contract = contracts.get(0);

        if (contract.getStatus() == ContractStatus.PENDING_RENEWAL) {
            throw new BusinessException("Hợp đồng của bạn đang có yêu cầu gia hạn chờ xét duyệt");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể xin gia hạn hợp đồng đang ở trạng thái ACTIVE");
        }

        boolean hasPending = renewalRequestRepository.existsByStudentIdAndStatus(studentId, RenewalStatus.SUBMITTED);
        if (hasPending) {
            throw new BusinessException("Bạn đang có một yêu cầu gia hạn đang chờ xét duyệt");
        }

        LocalDate targetRequestedEnd = requestedEnd;
        if (targetRequestedEnd == null) {
            int months = (termMonths != null && termMonths > 0) ? termMonths : 5;
            targetRequestedEnd = contract.getEndDate().plusMonths(months);
        }

        if (!targetRequestedEnd.isAfter(contract.getEndDate())) {
            throw new BusinessException("Thời hạn gia hạn mới phải sau ngày hết hạn hiện tại của hợp đồng (" + contract.getEndDate() + ")");
        }

        // Tạo đơn gia hạn
        RenewalRequest req = new RenewalRequest();
        req.setStudent(student);
        req.setContract(contract);
        req.setRequestedEnd(targetRequestedEnd);
        req.setStatus(RenewalStatus.SUBMITTED);
        req.setCreatedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            req.setAdminNote(note.length() > 500 ? note.substring(0, 500) : note);
        }
        RenewalRequest savedReq = renewalRequestRepository.save(req);

        // Chuyển trạng thái hợp đồng: ACTIVE -> PENDING_RENEWAL (§04-04)
        contract.setStatus(ContractStatus.PENDING_RENEWAL);
        contractRepository.save(contract);

        return savedReq;
    }

    @Override
    @Transactional
    public RenewalRequest approveRenewal(Long requestId, Long adminUserId, String adminNote) {
        RenewalRequest req = renewalRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu gia hạn #" + requestId));

        if (req.getStatus() != RenewalStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể duyệt đơn đang ở trạng thái SUBMITTED");
        }

        Contract contract = req.getContract();
        if (contract == null) {
            throw new BusinessException("Thông tin hợp đồng liên quan không hợp lệ");
        }

        // Quy tắc §04-04: Duyệt -> end_date = requested_end, PENDING_RENEWAL -> ACTIVE.
        contract.setEndDate(req.getRequestedEnd());
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);

        req.setStatus(RenewalStatus.APPROVED);
        req.setDecidedAt(LocalDateTime.now());
        if (adminNote != null && !adminNote.isBlank()) {
            req.setAdminNote(adminNote.trim());
        }
        return renewalRequestRepository.save(req);
    }

    @Override
    @Transactional
    public RenewalRequest rejectRenewal(Long requestId, Long adminUserId, String adminNote) {
        RenewalRequest req = renewalRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu gia hạn #" + requestId));

        if (req.getStatus() != RenewalStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể từ chối đơn đang ở trạng thái SUBMITTED");
        }

        Contract contract = req.getContract();
        // Quy tắc §04-04: REJECTED -> HĐ PENDING_RENEWAL -> ACTIVE, giữ end_date cũ (để SV checkout được).
        if (contract != null && contract.getStatus() == ContractStatus.PENDING_RENEWAL) {
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
        }

        req.setStatus(RenewalStatus.REJECTED);
        req.setDecidedAt(LocalDateTime.now());
        if (adminNote != null && !adminNote.isBlank()) {
            req.setAdminNote(adminNote.trim());
        }
        return renewalRequestRepository.save(req);
    }

    @Override
    @Transactional
    public RenewalRequest cancelRenewal(Long requestId, Long studentId) {
        RenewalRequest req = renewalRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu gia hạn #" + requestId));

        if (req.getStudent() == null || !req.getStudent().getId().equals(studentId)) {
            throw new BusinessException("Bạn không có quyền thao tác trên đơn gia hạn này");
        }

        if (req.getStatus() != RenewalStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể hủy đơn đang ở trạng thái SUBMITTED");
        }

        Contract contract = req.getContract();
        // Quy tắc §04-04: CANCELLED -> HĐ PENDING_RENEWAL -> ACTIVE, giữ end_date cũ.
        if (contract != null && contract.getStatus() == ContractStatus.PENDING_RENEWAL) {
            contract.setStatus(ContractStatus.ACTIVE);
            contractRepository.save(contract);
        }

        req.setStatus(RenewalStatus.CANCELLED);
        req.setDecidedAt(LocalDateTime.now());
        return renewalRequestRepository.save(req);
    }

    @Override
    @Transactional
    public int processExpiredContractsAndRenewals(LocalDate today) {
        int expiredCount = 0;

        // 1. PENDING_RENEWAL -> EXPIRED khi job thấy end_date < today và đơn gia hạn vẫn SUBMITTED; giường vẫn chiếm (§04-04 line 23)
        List<Contract> pendingRenewalsExpired = contractRepository.findByStatusAndEndDateBefore(ContractStatus.PENDING_RENEWAL, today);
        for (Contract c : pendingRenewalsExpired) {
            c.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(c);

            // Đơn SUBMITTED -> CANCELLED bởi job, ghi admin_note=EXPIRED (§5.2.14)
            List<RenewalRequest> pendingRequests = renewalRequestRepository.findByContractIdAndStatus(c.getId(), RenewalStatus.SUBMITTED);
            for (RenewalRequest r : pendingRequests) {
                r.setStatus(RenewalStatus.CANCELLED);
                r.setAdminNote("EXPIRED");
                r.setDecidedAt(LocalDateTime.now());
                renewalRequestRepository.save(r);
            }
            expiredCount++;
        }

        // 2. ACTIVE -> EXPIRED khi end_date < today (không gia hạn)
        List<Contract> activeExpired = contractRepository.findByStatusAndEndDateBefore(ContractStatus.ACTIVE, today);
        for (Contract c : activeExpired) {
            c.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(c);
            expiredCount++;
        }

        // 3. Nhắc nhở hết hạn trong vòng 30 ngày (§04-04 line 25)
        LocalDate notifyHorizon = today.plusDays(30);
        List<Contract> expiringSoon = contractRepository.findByStatusInAndEndDateLessThanEqual(
                List.of(ContractStatus.ACTIVE, ContractStatus.PENDING_RENEWAL), notifyHorizon);
        for (Contract c : expiringSoon) {
            if (c.getEndDate().isAfter(today.minusDays(1)) && c.getStudent() != null && c.getStudent().getUser() != null) {
                String title = "Thông báo: Hợp đồng " + c.getContractNo() + " sắp hết hạn";
                boolean alreadyNotified = notificationRepository.existsByUserIdAndTitle(c.getStudent().getUser().getId(), title);
                if (!alreadyNotified) {
                    Notification n = new Notification();
                    n.setUser(c.getStudent().getUser());
                    n.setTitle(title);
                    n.setBody("Hợp đồng phòng ở " + c.getContractNo() + " sẽ hết hạn vào ngày " + c.getEndDate() + ". Vui lòng nộp đơn gia hạn nếu bạn có nhu cầu tiếp tục lưu trú tại ký túc xá.");
                    n.setType(NotificationType.CONTRACT_EXPIRY);
                    n.setReadFlag(false);
                    n.setEmailSent(false);
                    n.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(n);
                }
            }
        }

        return expiredCount;
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalRequest getById(Long id) {
        return renewalRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu gia hạn #" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenewalRequest> findByStudentId(Long studentId) {
        return renewalRequestRepository.findByStudentIdWithDetails(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenewalRequest> searchRequests(RenewalStatus status, Long buildingId) {
        return renewalRequestRepository.searchRequests(status, buildingId);
    }
}