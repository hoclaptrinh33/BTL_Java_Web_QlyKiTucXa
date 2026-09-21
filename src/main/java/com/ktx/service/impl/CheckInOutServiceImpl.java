package com.ktx.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.CheckInOut;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.User;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.CheckInOutType;
import com.ktx.domain.enums.CompletionReason;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.CheckInOutRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.BillingEngine;
import com.ktx.service.CheckInOutService;

@Service
public class CheckInOutServiceImpl implements CheckInOutService {

    private final ContractRepository contractRepository;
    private final CheckInOutRepository checkInOutRepository;
    private final BedRepository bedRepository;
    private final UserRepository userRepository;
    private final RoomAssetRepository roomAssetRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingEngine billingEngine;

    public CheckInOutServiceImpl(ContractRepository contractRepository,
                                 CheckInOutRepository checkInOutRepository,
                                 BedRepository bedRepository,
                                 UserRepository userRepository,
                                 RoomAssetRepository roomAssetRepository,
                                 InvoiceRepository invoiceRepository,
                                 BillingEngine billingEngine) {
        this.contractRepository = contractRepository;
        this.checkInOutRepository = checkInOutRepository;
        this.bedRepository = bedRepository;
        this.userRepository = userRepository;
        this.roomAssetRepository = roomAssetRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingEngine = billingEngine;
    }

    @Override
    @Transactional
    public CheckInOut checkIn(Long contractId, Long staffUserId, String assetNote, Boolean ok, Map<Long, AssetCondition> assetConditions) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        // Quy tắc §04-04: Check-in chỉ từ DRAFT -> ACTIVE
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể check-in hợp đồng đang ở trạng thái DRAFT");
        }

        // Không check-in lần 2
        if (checkInOutRepository.existsByContractIdAndEventType(contractId, CheckInOutType.CHECK_IN)) {
            throw new BusinessException("Hợp đồng #" + contract.getContractNo() + " đã được check-in trước đó");
        }

        // Guard: Bed & Gender Policy (§04-04)
        if (contract.getBed() == null || contract.getBed().getRoom() == null || contract.getBed().getRoom().getBuilding() == null) {
            throw new BusinessException("Thông tin phòng/giường của hợp đồng không hợp lệ");
        }
        BuildingGenderPolicy genderPolicy = contract.getBed().getRoom().getBuilding().getGenderPolicy();
        if (genderPolicy != null && contract.getStudent() != null && contract.getStudent().getGender() != null) {
            if (!genderPolicy.name().equals(contract.getStudent().getGender().name())) {
                throw new BusinessException("Giới tính sinh viên không khớp với chính sách giới tính của tòa nhà");
            }
        }

        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin cán bộ thực hiện"));

        // Cập nhật tình trạng tài sản bàn giao nếu có
        if (assetConditions != null && !assetConditions.isEmpty()) {
            Long roomId = contract.getBed().getRoom().getId();
            for (Map.Entry<Long, AssetCondition> entry : assetConditions.entrySet()) {
                roomAssetRepository.findByIdAndRoomId(entry.getKey(), roomId).ifPresent(asset -> {
                    asset.setCondition(entry.getValue());
                    roomAssetRepository.save(asset);
                });
            }
        }

        // Tạo biên bản Check-in
        CheckInOut checkInOut = new CheckInOut();
        checkInOut.setContract(contract);
        checkInOut.setEventType(CheckInOutType.CHECK_IN);
        checkInOut.setPerformedAt(LocalDateTime.now());
        checkInOut.setPerformedBy(staffUser);
        checkInOut.setAssetNote(assetNote);
        checkInOut.setOk(ok != null ? ok : true);
        CheckInOut savedCheckInOut = checkInOutRepository.save(checkInOut);

        // Chuyển trạng thái hợp đồng DRAFT -> ACTIVE
        contract.setStatus(ContractStatus.ACTIVE);
        if (contract.getSignedAt() == null) {
            contract.setSignedAt(LocalDateTime.now());
        }
        contractRepository.save(contract);

        // Phát hành hóa đơn cọc (50% giá kỳ) và hóa đơn tiền phòng
        billingEngine.issueDeposit(contract.getId());
        billingEngine.issueRoomFee(contract.getId());

        return savedCheckInOut;
    }

    @Override
    @Transactional
    public CheckInOut checkOut(Long contractId, Long staffUserId, String assetNote, Boolean ok, DepositStatus depositDecision, boolean force, Map<Long, AssetCondition> assetConditions) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        ContractStatus currentStatus = contract.getStatus();
        // Quy tắc §04-04: Checkout từ ACTIVE | EXPIRED | TERMINATED -> COMPLETED
        if (currentStatus != ContractStatus.ACTIVE && currentStatus != ContractStatus.EXPIRED && currentStatus != ContractStatus.TERMINATED) {
            throw new BusinessException("Chỉ có thể check-out hợp đồng đang ở trạng thái ACTIVE, EXPIRED hoặc TERMINATED");
        }

        // Cảnh báo hóa đơn quá hạn (§04-04)
        if (contract.getStudent() != null) {
            boolean hasOverdue = invoiceRepository.existsByStudentIdAndStatus(contract.getStudent().getId(), InvoiceStatus.OVERDUE);
            if (hasOverdue && !force) {
                throw new BusinessException("Sinh viên còn hóa đơn QUÁ HẠN chưa thanh toán. Vui lòng xử lý công nợ hoặc dùng quyền quản trị để buộc check-out.");
            }
        }

        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin cán bộ thực hiện"));

        // Cập nhật tình trạng tài sản khi bàn giao trả phòng
        if (assetConditions != null && !assetConditions.isEmpty() && contract.getBed() != null && contract.getBed().getRoom() != null) {
            Long roomId = contract.getBed().getRoom().getId();
            for (Map.Entry<Long, AssetCondition> entry : assetConditions.entrySet()) {
                roomAssetRepository.findByIdAndRoomId(entry.getKey(), roomId).ifPresent(asset -> {
                    asset.setCondition(entry.getValue());
                    roomAssetRepository.save(asset);
                });
            }
        }

        // Tạo biên bản Check-out
        CheckInOut checkInOut = new CheckInOut();
        checkInOut.setContract(contract);
        checkInOut.setEventType(CheckInOutType.CHECK_OUT);
        checkInOut.setPerformedAt(LocalDateTime.now());
        checkInOut.setPerformedBy(staffUser);
        checkInOut.setAssetNote(assetNote);
        checkInOut.setOk(ok != null ? ok : true);
        CheckInOut savedCheckInOut = checkInOutRepository.save(checkInOut);

        // Chuyển hợp đồng sang COMPLETED
        contract.setStatus(ContractStatus.COMPLETED);
        if (currentStatus == ContractStatus.TERMINATED) {
            contract.setCompletionReason(CompletionReason.FORCED_AFTER_CHECKOUT);
        } else {
            contract.setCompletionReason(CompletionReason.NORMAL_CHECKOUT);
        }

        // Cập nhật trạng thái cọc
        if (depositDecision != null) {
            contract.setDepositStatus(depositDecision);
        } else if (Boolean.FALSE.equals(ok)) {
            contract.setDepositStatus(DepositStatus.FORFEITED);
        } else {
            contract.setDepositStatus(DepositStatus.REFUNDED);
        }
        contractRepository.save(contract);

        // Nhả giường về VACANT và xóa current_contract_id (§04-04, §5.2.6)
        if (contract.getBed() != null) {
            int vacated = bedRepository.vacateBed(contract.getBed().getId());
            if (vacated != 1) {
                throw new BusinessException("Không thể nhả giường #" + contract.getBed().getId()
                        + ", giường không ở trạng thái OCCUPIED");
            }
        }

        return savedCheckInOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInOut> findByContractId(Long contractId) {
        return checkInOutRepository.findByContractIdOrderByPerformedAtDesc(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckInOut> findRecent(Long buildingId) {
        return checkInOutRepository.findRecentWithDetails(buildingId);
    }
}
