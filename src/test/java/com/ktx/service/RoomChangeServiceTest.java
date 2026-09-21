package com.ktx.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.Gender;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RoomChangeRequestRepository;
import com.ktx.repository.StudentRepository;
import com.ktx.service.impl.RoomChangeServiceImpl;

@ExtendWith(MockitoExtension.class)
class RoomChangeServiceTest {

    @Mock
    private RoomChangeRequestRepository roomChangeRequestRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private CheckInOutService checkInOutService;

    private RoomChangeService roomChangeService;

    private Student maleStudent;
    private Student femaleStudent;
    private Building buildingMale;
    private Building buildingFemale;
    private Room roomMale;
    private Room roomFemale;
    private Bed bedMale1;
    private Bed bedMale2;
    private Bed bedFemale1;
    private Contract contractMale;

    @BeforeEach
    void setUp() {
        roomChangeService = new RoomChangeServiceImpl(
                roomChangeRequestRepository,
                bedRepository,
                contractRepository,
                studentRepository,
                buildingRepository,
                checkInOutService
        );

        maleStudent = new Student();
        maleStudent.setId(1L);
        maleStudent.setFullName("Nguyen Van A");
        maleStudent.setGender(Gender.MALE);

        femaleStudent = new Student();
        femaleStudent.setId(2L);
        femaleStudent.setFullName("Tran Thi B");
        femaleStudent.setGender(Gender.FEMALE);

        buildingMale = new Building();
        buildingMale.setId(10L);
        buildingMale.setCode("A");
        buildingMale.setName("Tòa A");
        buildingMale.setGenderPolicy(BuildingGenderPolicy.MALE);

        buildingFemale = new Building();
        buildingFemale.setId(20L);
        buildingFemale.setCode("B");
        buildingFemale.setName("Tòa B");
        buildingFemale.setGenderPolicy(BuildingGenderPolicy.FEMALE);

        roomMale = new Room();
        roomMale.setId(101L);
        roomMale.setRoomNumber("101");
        roomMale.setBuilding(buildingMale);
        roomMale.setRoomType(RoomType.STANDARD_4);

        roomFemale = new Room();
        roomFemale.setId(201L);
        roomFemale.setRoomNumber("201");
        roomFemale.setBuilding(buildingFemale);
        roomFemale.setRoomType(RoomType.STANDARD_4);

        bedMale1 = new Bed();
        bedMale1.setId(5L);
        bedMale1.setBedCode("G1");
        bedMale1.setStatus(BedStatus.OCCUPIED);
        bedMale1.setRoom(roomMale);

        bedMale2 = new Bed();
        bedMale2.setId(8L);
        bedMale2.setBedCode("G2");
        bedMale2.setStatus(BedStatus.VACANT);
        bedMale2.setRoom(roomMale);

        bedFemale1 = new Bed();
        bedFemale1.setId(15L);
        bedFemale1.setBedCode("G1");
        bedFemale1.setStatus(BedStatus.VACANT);
        bedFemale1.setRoom(roomFemale);

        contractMale = new Contract();
        contractMale.setId(100L);
        contractMale.setContractNo("HD-001");
        contractMale.setStatus(ContractStatus.ACTIVE);
        contractMale.setStudent(maleStudent);
        contractMale.setBed(bedMale1);
    }

    @Test
    @DisplayName("Nộp đơn đổi phòng thành công khi có HĐ ACTIVE và đúng chính sách giới tính")
    void submitRoomChangeRequest_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(maleStudent));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(1L), any()))
                .thenReturn(List.of(contractMale));
        when(roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(eq(1L), eq(RoomChangeKind.CHANGE), any()))
                .thenReturn(false);
        when(buildingRepository.findById(10L)).thenReturn(Optional.of(buildingMale));
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomChangeRequest req = roomChangeService.submitRoomChangeRequest(1L, 10L, RoomType.STANDARD_4, "Chuyển sang tầng 2");

        assertNotNull(req);
        assertEquals(RoomChangeKind.CHANGE, req.getRequestKind());
        assertEquals(RoomChangeStatus.SUBMITTED, req.getStatus());
        assertEquals(maleStudent, req.getStudent());
        assertEquals(contractMale, req.getContract());
        assertEquals(bedMale1, req.getCurrentBed());
        assertEquals("Chuyển sang tầng 2", req.getReason());
    }

    @Test
    @DisplayName("Nộp đơn đổi phòng thất bại nếu không có hợp đồng ACTIVE")
    void submitRoomChangeRequest_fail_noActiveContract() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(maleStudent));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(1L), any()))
                .thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                roomChangeService.submitRoomChangeRequest(1L, null, RoomType.STANDARD_4, "Lý do"));
        assertTrue(ex.getMessage().contains("ACTIVE"));
    }

    @Test
    @DisplayName("Nộp đơn đổi phòng thất bại nếu đã có đơn SUBMITTED đang chờ")
    void submitRoomChangeRequest_fail_alreadyPending() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(maleStudent));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(1L), any()))
                .thenReturn(List.of(contractMale));
        when(roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(eq(1L), eq(RoomChangeKind.CHANGE), any()))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                roomChangeService.submitRoomChangeRequest(1L, null, RoomType.STANDARD_4, "Lý do"));
        assertTrue(ex.getMessage().contains("đang chờ xử lý"));
    }

    @Test
    @DisplayName("Nộp đơn đổi phòng thất bại nếu nam xin vào tòa nữ (gender policy guard)")
    void submitRoomChangeRequest_fail_genderPolicyMismatch() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(maleStudent));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(1L), any()))
                .thenReturn(List.of(contractMale));
        when(roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(eq(1L), eq(RoomChangeKind.CHANGE), any()))
                .thenReturn(false);
        when(buildingRepository.findById(20L)).thenReturn(Optional.of(buildingFemale));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                roomChangeService.submitRoomChangeRequest(1L, 20L, RoomType.STANDARD_4, "Lý do"));
        assertTrue(ex.getMessage().contains("Giới tính sinh viên không khớp"));
    }

    @Test
    @DisplayName("Nộp đơn trả phòng thành công lưu đầy đủ lý do, ngày dự kiến và tài khoản hoàn cọc")
    void submitReturnRoomRequest_success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(maleStudent));
        when(contractRepository.findByStudentIdAndStatusInWithDetails(eq(1L), any()))
                .thenReturn(List.of(contractMale));
        when(roomChangeRequestRepository.existsByStudentIdAndRequestKindAndStatusIn(eq(1L), eq(RoomChangeKind.RETURN), any()))
                .thenReturn(false);
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomChangeRequest req = roomChangeService.submitReturnRoomRequest(1L, "2026-06-30", "Tốt nghiệp ra trường", "MB Bank", "0987654321");

        assertNotNull(req);
        assertEquals(RoomChangeKind.RETURN, req.getRequestKind());
        assertEquals(RoomChangeStatus.SUBMITTED, req.getStatus());
        assertTrue(req.getReason().contains("Tốt nghiệp ra trường"));
        assertTrue(req.getReason().contains("2026-06-30"));
        assertTrue(req.getReason().contains("0987654321"));
    }

    @Test
    @DisplayName("Hủy đơn thành công khi đơn đang ở trạng thái SUBMITTED")
    void cancelRequest_success() {
        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(10L);
        req.setStudent(maleStudent);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findById(10L)).thenReturn(Optional.of(req));
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class))).thenAnswer(i -> i.getArgument(0));

        RoomChangeRequest cancelled = roomChangeService.cancelRequest(10L, 1L);
        assertEquals(RoomChangeStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("Không được hủy đơn của sinh viên khác")
    void cancelRequest_fail_wrongStudent() {
        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(10L);
        req.setStudent(femaleStudent);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findById(10L)).thenReturn(Optional.of(req));

        assertThrows(BusinessException.class, () -> roomChangeService.cancelRequest(10L, 1L));
    }

    @Test
    @DisplayName("Duyệt đổi phòng thành công: Khóa 2 giường ORDER BY id ASC tránh deadlock và cập nhật HĐ")
    void approveAndExecuteRoomChange_success_locksTwoBedsInAscendingOrder() {
        // currentBedId = 8, targetBedId = 5 (ngược thứ tự id để kiểm tra việc sort)
        bedMale1.setId(8L);
        bedMale2.setId(5L);
        contractMale.setBed(bedMale1);

        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(100L);
        req.setStudent(maleStudent);
        req.setContract(contractMale);
        req.setCurrentBed(bedMale1);
        req.setRequestKind(RoomChangeKind.CHANGE);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(req));

        // Mock khóa 2 giường theo thứ tự [5L, 8L]
        when(bedRepository.findByIdInForUpdate(List.of(5L, 8L))).thenReturn(List.of(bedMale2, bedMale1));
        when(bedRepository.vacateBed(8L)).thenReturn(1);
        when(bedRepository.occupyBed(5L, 100L)).thenReturn(1);
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class))).thenAnswer(i -> i.getArgument(0));

        RoomChangeRequest result = roomChangeService.approveAndExecuteRoomChange(100L, 5L, 99L, "Duyệt chuyển phòng");

        // Verify: khóa 2 giường đúng danh sách được sắp xếp tăng dần!
        verify(bedRepository).findByIdInForUpdate(List.of(5L, 8L));
        verify(bedRepository).vacateBed(8L);
        verify(bedRepository).occupyBed(5L, 100L);
        verify(contractRepository).save(contractMale);

        assertEquals(RoomChangeStatus.COMPLETED, result.getStatus());
        assertEquals(bedMale2, result.getTargetBed());
        assertEquals(bedMale2, contractMale.getBed());
        assertEquals("Duyệt chuyển phòng", result.getAdminNote());
    }

    @Test
    @DisplayName("Duyệt đổi phòng thất bại nếu giường mới là MAINTENANCE")
    void approveAndExecuteRoomChange_fail_targetBedMaintenance() {
        bedMale2.setStatus(BedStatus.MAINTENANCE);

        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(100L);
        req.setStudent(maleStudent);
        req.setContract(contractMale);
        req.setCurrentBed(bedMale1);
        req.setRequestKind(RoomChangeKind.CHANGE);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(req));
        when(bedRepository.findByIdInForUpdate(List.of(5L, 8L))).thenReturn(List.of(bedMale1, bedMale2));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                roomChangeService.approveAndExecuteRoomChange(100L, 8L, 99L, null));
        assertTrue(ex.getMessage().contains("bảo trì"));
    }

    @Test
    @DisplayName("Duyệt đổi phòng thất bại nếu giường mới sang tòa khác giới tính")
    void approveAndExecuteRoomChange_fail_genderPolicyMismatch() {
        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(100L);
        req.setStudent(maleStudent);
        req.setContract(contractMale);
        req.setCurrentBed(bedMale1);
        req.setRequestKind(RoomChangeKind.CHANGE);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(req));
        // bedMale1 id=5, bedFemale1 id=15
        when(bedRepository.findByIdInForUpdate(List.of(5L, 15L))).thenReturn(List.of(bedMale1, bedFemale1));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                roomChangeService.approveAndExecuteRoomChange(100L, 15L, 99L, null));
        assertTrue(ex.getMessage().contains("khác chính sách giới tính"));
    }

    @Test
    @DisplayName("Duyệt trả phòng thành công gọi trực tiếp CheckInOutService.checkOut")
    void approveReturnRoom_success_callsCheckout() {
        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(200L);
        req.setStudent(maleStudent);
        req.setContract(contractMale);
        req.setCurrentBed(bedMale1);
        req.setRequestKind(RoomChangeKind.RETURN);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findByIdWithDetails(200L)).thenReturn(Optional.of(req));
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class))).thenAnswer(i -> i.getArgument(0));

        RoomChangeRequest result = roomChangeService.approveReturnRoom(
                200L, 99L, "Đã kiểm kê phòng sạch sẽ", true, DepositStatus.REFUNDED, false, null
        );

        // Verify checkOut được gọi đúng contractId và các tham số
        verify(checkInOutService).checkOut(
                eq(100L), eq(99L), eq("Đã kiểm kê phòng sạch sẽ"), eq(true), eq(DepositStatus.REFUNDED), eq(false), any()
        );

        assertEquals(RoomChangeStatus.COMPLETED, result.getStatus());
        assertEquals("Đã kiểm kê phòng sạch sẽ", result.getAdminNote());
    }

    @Test
    @DisplayName("Từ chối đơn thành công cập nhật trạng thái REJECTED")
    void rejectRequest_success() {
        RoomChangeRequest req = new RoomChangeRequest();
        req.setId(300L);
        req.setStatus(RoomChangeStatus.SUBMITTED);

        when(roomChangeRequestRepository.findById(300L)).thenReturn(Optional.of(req));
        when(roomChangeRequestRepository.save(any(RoomChangeRequest.class))).thenAnswer(i -> i.getArgument(0));

        RoomChangeRequest result = roomChangeService.rejectRequest(300L, 99L, "Hết phòng trống phù hợp");

        assertEquals(RoomChangeStatus.REJECTED, result.getStatus());
        assertEquals("Hết phòng trống phù hợp", result.getAdminNote());
    }
}