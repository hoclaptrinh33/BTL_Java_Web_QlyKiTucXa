package com.ktx.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Room;
import com.ktx.domain.User;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.dto.UtilityReadingForm;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.UserRepository;
import com.ktx.repository.UtilityReadingRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.UtilityReadingService;

@Service
public class UtilityReadingServiceImpl implements UtilityReadingService {

    private final UtilityReadingRepository utilityReadingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final StaffScope staffScope;

    public UtilityReadingServiceImpl(UtilityReadingRepository utilityReadingRepository,
                                     RoomRepository roomRepository,
                                     UserRepository userRepository,
                                     InvoiceRepository invoiceRepository,
                                     StaffScope staffScope) {
        this.utilityReadingRepository = utilityReadingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.staffScope = staffScope;
    }

    @Override
    @Transactional
    public UtilityReading recordReading(UtilityReadingForm form, Authentication auth) {
        if (form.getRoomId() == null) {
            throw new BusinessException("Mã phòng không được để trống");
        }
        if (form.getBillingMonth() == null || form.getBillingMonth().isBlank()) {
            throw new BusinessException("Tháng ghi chỉ số không được để trống");
        }

        Room room = roomRepository.findById(form.getRoomId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + form.getRoomId()));

        // Phân quyền StaffScope: staff chỉ ghi chỉ số phòng thuộc tòa của mình
        staffScope.assertRoom(auth, room);

        YearMonth ym;
        try {
            ym = YearMonth.parse(form.getBillingMonth().trim());
        } catch (Exception e) {
            throw new BusinessException("Định dạng tháng không hợp lệ (yêu cầu YYYY-MM)");
        }
        LocalDate billingMonth = ym.atDay(1);

        // Kiểm tra xem đã phát hành hóa đơn chưa: nếu có hóa đơn chưa hủy -> khóa sửa
        boolean hasActiveInvoice = invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                room.getId(), billingMonth, InvoiceType.UTILITY, InvoiceStatus.CANCELLED);
        if (hasActiveInvoice) {
            throw new BusinessException("Không thể chỉnh sửa chỉ số: Phòng đã được phát hành hóa đơn điện nước tháng " + form.getBillingMonth());
        }

        User actor = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng " + auth.getName()));

        UtilityReading reading = utilityReadingRepository.findByRoomIdAndBillingMonth(room.getId(), billingMonth)
                .orElseGet(UtilityReading::new);

        reading.setRoom(room);
        reading.setBillingMonth(billingMonth);
        reading.setElecPrev(form.getElecPrev());
        reading.setElecCurr(form.getElecCurr());
        reading.setElecReplaced(Boolean.TRUE.equals(form.getElecReplaced()));
        reading.setElecOldFinal(form.getElecOldFinal());
        reading.setElecNewStart(form.getElecNewStart());

        reading.setWaterPrev(form.getWaterPrev());
        reading.setWaterCurr(form.getWaterCurr());
        reading.setWaterReplaced(Boolean.TRUE.equals(form.getWaterReplaced()));
        reading.setWaterOldFinal(form.getWaterOldFinal());
        reading.setWaterNewStart(form.getWaterNewStart());

        reading.setNewBuildingMeter(Boolean.TRUE.equals(form.getNewBuildingMeter()));
        reading.setRecordedBy(actor);
        reading.setRecordedAt(LocalDateTime.now());

        // Validate tính hợp lệ theo công thức tiêu thụ
        reading.calculateKwh();
        reading.calculateM3();

        return utilityReadingRepository.save(reading);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityReadingForm prepareForm(Long roomId, YearMonth month) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + roomId));

        LocalDate billingMonth = month.atDay(1);
        UtilityReadingForm form = new UtilityReadingForm();
        form.setRoomId(roomId);
        form.setBillingMonth(month.toString());

        Optional<UtilityReading> currentOpt = utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth);
        if (currentOpt.isPresent()) {
            UtilityReading current = currentOpt.get();
            form.setElecPrev(current.getElecPrev());
            form.setElecCurr(current.getElecCurr());
            form.setElecReplaced(current.getElecReplaced());
            form.setElecOldFinal(current.getElecOldFinal());
            form.setElecNewStart(current.getElecNewStart());

            form.setWaterPrev(current.getWaterPrev());
            form.setWaterCurr(current.getWaterCurr());
            form.setWaterReplaced(current.getWaterReplaced());
            form.setWaterOldFinal(current.getWaterOldFinal());
            form.setWaterNewStart(current.getWaterNewStart());

            form.setNewBuildingMeter(current.getNewBuildingMeter());
        } else {
            // Mặc định prev từ curr của tháng trước (hoặc đọc gần nhất)
            LocalDate prevMonth = month.minusMonths(1).atDay(1);
            Optional<UtilityReading> prevOpt = utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, prevMonth);
            if (prevOpt.isPresent()) {
                form.setElecPrev(prevOpt.get().getElecCurr());
                form.setWaterPrev(prevOpt.get().getWaterCurr());
            } else {
                Optional<UtilityReading> latestOpt = utilityReadingRepository.findTopByRoomIdOrderByBillingMonthDesc(roomId);
                if (latestOpt.isPresent()) {
                    form.setElecPrev(latestOpt.get().getElecCurr());
                    form.setWaterPrev(latestOpt.get().getWaterCurr());
                } else {
                    form.setElecPrev(0);
                    form.setWaterPrev(0);
                }
            }
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilityReading> getReading(Long roomId, YearMonth month) {
        return utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, month.atDay(1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilityReading> getReadingsByBuilding(Long buildingId, YearMonth month) {
        return utilityReadingRepository.findByBuildingIdAndBillingMonth(buildingId, month.atDay(1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilityReading> getAllReadingsByMonth(YearMonth month) {
        return utilityReadingRepository.findByBillingMonth(month.atDay(1));
    }
}
