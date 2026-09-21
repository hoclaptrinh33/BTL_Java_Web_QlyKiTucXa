package com.ktx.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.Student;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RoomChangeRequestRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.CheckInOutService;
import com.ktx.service.RoomChangeService;

@Service
public class RoomChangeServiceImpl implements RoomChangeService {

    private final RoomChangeRequestRepository roomChangeRequestRepository;
    private final BedRepository bedRepository;
    private final ContractRepository contractRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final CheckInOutService checkInOutService;

    public RoomChangeServiceImpl(RoomChangeRequestRepository roomChangeRequestRepository,
                                 BedRepository bedRepository,
                                 ContractRepository contractRepository,
                                 StudentRepository studentRepository,
                                 BuildingRepository buildingRepository,
                                 CheckInOutService checkInOutService) {
        this.roomChangeRequestRepository = roomChangeRequestRepository;
        this.bedRepository = bedRepository;
        this.contractRepository = contractRepository;
        this.studentRepository = studentRepository;
        this.buildingRepository = buildingRepository;
        this.checkInOutService = checkInOutService;
    }

    @Override
    @Transactional
    public RoomChangeRequest submitRoomChangeRequest(Long studentId, Long requestedBuildingId, RoomType requestedRoomType, String reason) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên #" + studentId));

        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                studentId, OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Bạn không có hợp đồng phòng ở đang hoạt động (ACTIVE) để xin chuyển phòng"));

        if (activeContract.getBed() == null) {
            throw new BusinessException("Hợp đồng hiện tại chưa được gán giường");
        }

        boolean hasPending = roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(
                studentId, RoomChangeKind.CHANGE, List.of(RoomChangeStatus.SUBMITTED));
        if (hasPending) {
            throw new BusinessException("Bạn đang có một yêu cầu chuyển phòng đang chờ xử lý");
        }

        Building requestedBuilding = null;
        if (requestedBuildingId != null) {
            requestedBuilding = buildingRepository.findById(requestedBuildingId)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tòa nhà mong muốn #" + requestedBuildingId));
            BuildingGenderPolicy genderPolicy = requestedBuilding.getGenderPolicy();
            if (genderPolicy != null && student.getGender() != null) {
                if (!genderPolicy.name().equals(student.getGender().name())) {
                    throw new BusinessException("Giới tính sinh viên không khớp với chính sách giới tính của tòa nhà mong muốn");
                }
            }
        }

        RoomChangeRequest request = new RoomChangeRequest();
        request.setStudent(student);
        request.setContract(activeContract);
        request.setCurrentBed(activeContract.getBed());
        request.setRequestKind(RoomChangeKind.CHANGE);
        request.setRequestedBuilding(requestedBuilding);
        request.setRequestedRoomType(requestedRoomType);
        request.setReason(reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason);
        request.setStatus(RoomChangeStatus.SUBMITTED);
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RoomChangeRequest submitReturnRoomRequest(Long studentId, String returnDate, String reason, String bankName, String bankAccount) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sinh viên #" + studentId));

        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                studentId, OccupyingStatuses.OCCUPYING);
        if (contracts.isEmpty()) {
            throw new BusinessException("Bạn không có hợp đồng phòng ở nào cần làm thủ tục trả phòng");
        }
        Contract contract = contracts.get(0);

        if (contract.getBed() == null) {
            throw new BusinessException("Hợp đồng chưa được gán giường");
        }

        boolean hasPending = roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(
                studentId, RoomChangeKind.RETURN, List.of(RoomChangeStatus.SUBMITTED));
        if (hasPending) {
            throw new BusinessException("Bạn đang có một yêu cầu trả phòng đang chờ xử lý");
        }

        StringBuilder sb = new StringBuilder();
        if (reason != null && !reason.isBlank()) {
            sb.append(reason.trim());
        }
        if (returnDate != null && !returnDate.isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Ngày dự kiến: ").append(returnDate.trim());
        }
        if (bankAccount != null && !bankAccount.isBlank()) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Hoàn cọc: ").append(bankName != null ? bankName.trim() : "").append(" - ").append(bankAccount.trim());
        }
        String combinedReason = sb.toString();
        if (combinedReason.length() > 500) {
            combinedReason = combinedReason.substring(0, 500);
        }

        RoomChangeRequest request = new RoomChangeRequest();
        request.setStudent(student);
        request.setContract(contract);
        request.setCurrentBed(contract.getBed());
        request.setRequestKind(RoomChangeKind.RETURN);
        request.setReason(combinedReason);
        request.setStatus(RoomChangeStatus.SUBMITTED);
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RoomChangeRequest cancelRequest(Long requestId, Long studentId) {
        RoomChangeRequest request = roomChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu #" + requestId));

        if (request.getStudent() == null || !request.getStudent().getId().equals(studentId)) {
            throw new BusinessException("Bạn không có quyền thao tác trên yêu cầu này");
        }

        if (request.getStatus() != RoomChangeStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể hủy yêu cầu đang ở trạng thái SUBMITTED");
        }

        request.setStatus(RoomChangeStatus.CANCELLED);
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RoomChangeRequest approveAndExecuteRoomChange(Long requestId, Long targetBedId, Long adminUserId, String adminNote) {
        RoomChangeRequest request = roomChangeRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu chuyển phòng #" + requestId));

        if (request.getStatus() != RoomChangeStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể duyệt đơn đang ở trạng thái SUBMITTED");
        }
        if (request.getRequestKind() != RoomChangeKind.CHANGE) {
            throw new BusinessException("Đơn không phải là đơn chuyển phòng (CHANGE)");
        }
        if (targetBedId == null) {
            throw new BusinessException("Vui lòng chọn giường mới cần chuyển đến");
        }

        Long currentBedId = request.getCurrentBed().getId();
        if (currentBedId.equals(targetBedId)) {
            throw new BusinessException("Giường mới trùng với giường hiện tại của sinh viên");
        }

        // Quy tắc quan trọng: Khóa 2 giường ORDER BY id ASC tránh deadlock (§5.2.13, PR-10)
        List<Long> bedIdsSorted = Stream.of(currentBedId, targetBedId).sorted().toList();
        List<Bed> lockedBeds = bedRepository.findByIdInForUpdate(bedIdsSorted);
        if (lockedBeds.size() != 2) {
            throw new BusinessException("Không tìm thấy đủ 2 giường liên quan trong hệ thống");
        }

        Bed currentBed = lockedBeds.stream().filter(b -> b.getId().equals(currentBedId)).findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin giường hiện tại"));
        Bed targetBed = lockedBeds.stream().filter(b -> b.getId().equals(targetBedId)).findFirst()
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin giường mới"));

        // Kiểm tra giường bảo trì
        if (targetBed.getStatus() == BedStatus.MAINTENANCE) {
            throw new BusinessException("Không thể chuyển sang giường đang bảo trì (MAINTENANCE)");
        }
        // Kiểm tra giường còn trống
        if (targetBed.getStatus() != BedStatus.VACANT) {
            throw new BusinessException("Giường mới #" + targetBed.getBedCode() + " hiện không còn trống (VACANT)");
        }

        // Kiểm tra chính sách giới tính của tòa nhà mới
        if (targetBed.getRoom() == null || targetBed.getRoom().getBuilding() == null) {
            throw new BusinessException("Thông tin phòng hoặc tòa nhà của giường mới không hợp lệ");
        }
        BuildingGenderPolicy genderPolicy = targetBed.getRoom().getBuilding().getGenderPolicy();
        if (genderPolicy != null && request.getStudent() != null && request.getStudent().getGender() != null) {
            if (!genderPolicy.name().equals(request.getStudent().getGender().name())) {
                throw new BusinessException("Không thể chuyển sang tòa nhà khác chính sách giới tính của sinh viên");
            }
        }

        // Kiểm tra trạng thái hợp đồng
        Contract contract = request.getContract();
        if (contract == null || contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể chuyển phòng cho hợp đồng đang ở trạng thái ACTIVE");
        }

        // Trong cùng Transaction: nhả giường cũ, chiếm giường mới, cập nhật hợp đồng
        int vacated = bedRepository.vacateBed(currentBed.getId());
        if (vacated != 1) {
            throw new BusinessException("Không thể giải phóng giường cũ #" + currentBed.getId());
        }

        int occupied = bedRepository.occupyBed(targetBed.getId(), contract.getId());
        if (occupied != 1) {
            throw new BusinessException("Không thể chiếm giữ giường mới #" + targetBed.getId());
        }

        contract.setBed(targetBed);
        contractRepository.save(contract);

        request.setTargetBed(targetBed);
        request.setStatus(RoomChangeStatus.COMPLETED);
        if (adminNote != null && !adminNote.isBlank()) {
            request.setAdminNote(adminNote.trim());
        }
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RoomChangeRequest approveReturnRoom(Long requestId, Long staffUserId, String assetNote, Boolean ok, DepositStatus depositDecision, boolean force, Map<Long, AssetCondition> assetConditions) {
        RoomChangeRequest request = roomChangeRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu trả phòng #" + requestId));

        if (request.getStatus() != RoomChangeStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể duyệt đơn đang ở trạng thái SUBMITTED");
        }
        if (request.getRequestKind() != RoomChangeKind.RETURN) {
            throw new BusinessException("Đơn không phải là đơn đăng ký trả phòng (RETURN)");
        }

        // Gọi trực tiếp checkInOutService.checkOut để tận dụng toàn bộ quy trình checkout
        checkInOutService.checkOut(
                request.getContract().getId(),
                staffUserId,
                assetNote != null && !assetNote.isBlank() ? assetNote : "Trả phòng theo đơn #" + request.getId(),
                ok,
                depositDecision,
                force,
                assetConditions
        );

        request.setStatus(RoomChangeStatus.COMPLETED);
        if (assetNote != null && !assetNote.isBlank()) {
            request.setAdminNote(assetNote.trim());
        }
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional
    public RoomChangeRequest rejectRequest(Long requestId, Long adminUserId, String adminNote) {
        RoomChangeRequest request = roomChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu #" + requestId));

        if (request.getStatus() != RoomChangeStatus.SUBMITTED) {
            throw new BusinessException("Chỉ có thể từ chối đơn đang ở trạng thái SUBMITTED");
        }

        request.setStatus(RoomChangeStatus.REJECTED);
        if (adminNote != null && !adminNote.isBlank()) {
            request.setAdminNote(adminNote.trim());
        }
        return roomChangeRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomChangeRequest getById(Long id) {
        return roomChangeRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu #" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomChangeRequest> findByStudentIdAndKind(Long studentId, RoomChangeKind kind) {
        return roomChangeRequestRepository.findByStudentIdAndRequestKindWithDetails(studentId, kind);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomChangeRequest> searchRequests(RoomChangeKind kind, RoomChangeStatus status, Long buildingId) {
        return roomChangeRequestRepository.searchRequests(kind, status, buildingId);
    }
}