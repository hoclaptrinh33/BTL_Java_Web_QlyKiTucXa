package com.ktx.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.enums.CompletionReason;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.service.ContractService;
import com.ktx.service.DocumentNumberService;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final BedRepository bedRepository;
    private final DocumentNumberService documentNumberService;

    public ContractServiceImpl(ContractRepository contractRepository,
                               BedRepository bedRepository,
                               DocumentNumberService documentNumberService) {
        this.contractRepository = contractRepository;
        this.bedRepository = bedRepository;
        this.documentNumberService = documentNumberService;
    }

    @Override
    @Transactional
    public Contract createDraftFromAllocation(RoomApplication app, Bed bed, LocalDate termStart, LocalDate termEnd) {
        if (app == null) {
            throw new BusinessException("Thông tin đơn không hợp lệ");
        }
        return createDraft(app.getStudent(), bed, app, termStart, termEnd);
    }

    @Override
    @Transactional
    public Contract createDraft(com.ktx.domain.Student student, Bed bed, RoomApplication app, LocalDate termStart, LocalDate termEnd) {
        if (student == null || bed == null || termStart == null || termEnd == null) {
            throw new BusinessException("Thông tin sinh viên, giường hoặc thời hạn kỳ không hợp lệ");
        }

        BigDecimal pricePerTerm = BigDecimal.ZERO;
        if (bed.getRoom() != null && bed.getRoom().getPricePerTerm() != null) {
            pricePerTerm = bed.getRoom().getPricePerTerm();
        }

        // Cọc: 50% giá phòng/kỳ làm tròn HALF_UP ra số nguyên VND (§04-04)
        BigDecimal depositAmount = pricePerTerm.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP);

        int year = termStart.getYear();
        String contractNo = documentNumberService.nextContractNo(year);

        Contract contract = new Contract();
        contract.setContractNo(contractNo);
        contract.setStudent(student);
        contract.setBed(bed);
        contract.setApplication(app);
        contract.setStartDate(termStart);
        contract.setEndDate(termEnd);
        contract.setRoomFee(pricePerTerm);
        contract.setDepositAmount(depositAmount);
        contract.setDepositStatus(DepositStatus.HELD);
        contract.setStatus(ContractStatus.DRAFT);

        // 1. INSERT contract
        Contract savedContract = contractRepository.save(contract);

        // 2. UPDATE bed status = OCCUPIED, currentContractId = contract.id (§5.2.6)
        int updated = bedRepository.occupyBed(bed.getId(), savedContract.getId());
        if (updated != 1) {
            throw new BusinessException("Không thể khóa giường #" + bed.getId() + " (" + bed.getBedCode() + "), giường đã bị chiếm dụng hoặc không còn VACANT");
        }

        return savedContract;
    }

    @Override
    @Transactional
    public void cancelDraft(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessException("Chỉ có thể hủy hợp đồng đang ở trạng thái DRAFT");
        }

        // Đổi trạng thái DRAFT -> COMPLETED với lý do CANCELLED_BEFORE_CHECKIN (§04-04)
        contract.setStatus(ContractStatus.COMPLETED);
        contract.setCompletionReason(CompletionReason.CANCELLED_BEFORE_CHECKIN);
        contractRepository.save(contract);

        // Nhả giường về VACANT
        if (contract.getBed() != null) {
            bedRepository.vacateBed(contract.getBed().getId());
        }
    }
}
