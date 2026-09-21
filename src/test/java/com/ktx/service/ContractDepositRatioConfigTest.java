package com.ktx.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.Bed;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BedStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.service.impl.ContractServiceImpl;

@ExtendWith(MockitoExtension.class)
class ContractDepositRatioConfigTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private DocumentNumberService documentNumberService;

    @Mock
    private SystemConfigService systemConfigService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractServiceImpl(
                contractRepository,
                bedRepository,
                documentNumberService,
                systemConfigService
        );
    }

    @Test
    @DisplayName("Changing contract.deposit.ratio takes dynamic effect on draft creation")
    void createDraft_usesDynamicDepositRatioFromConfig() {
        Student student = new Student();
        student.setId(1L);

        Room room = new Room();
        room.setId(2L);
        room.setPricePerTerm(new BigDecimal("2000000")); // 2.000.000 VND

        Bed bed = new Bed();
        bed.setId(3L);
        bed.setBedCode("G-01");
        bed.setStatus(BedStatus.VACANT);
        bed.setRoom(room);

        RoomApplication app = new RoomApplication();
        app.setId(4L);
        app.setStudent(student);

        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2027, 1, 31);

        when(documentNumberService.nextContractNo(2026)).thenReturn("HD-2026-000099");
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(bedRepository.occupyBed(3L, 10L)).thenReturn(1);

        // Configure ratio to 0.3 (30%)
        when(systemConfigService.getBigDecimal(eq("contract.deposit.ratio"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.3"));

        Contract contract = contractService.createDraftFromAllocation(app, bed, start, end);

        assertNotNull(contract);
        assertEquals(new BigDecimal("2000000"), contract.getRoomFee());
        // 30% of 2.000.000 = 600.000 VND
        assertEquals(new BigDecimal("600000"), contract.getDepositAmount());
        verify(bedRepository).occupyBed(3L, 10L);
    }

    @Test
    @DisplayName("When contract.deposit.ratio is 0.5 (50%), deposit amount is half room fee")
    void createDraft_defaultDepositRatio() {
        Student student = new Student();
        student.setId(1L);

        Room room = new Room();
        room.setId(2L);
        room.setPricePerTerm(new BigDecimal("1500000"));

        Bed bed = new Bed();
        bed.setId(3L);
        bed.setBedCode("G-01");
        bed.setStatus(BedStatus.VACANT);
        bed.setRoom(room);

        RoomApplication app = new RoomApplication();
        app.setId(4L);
        app.setStudent(student);

        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2027, 1, 31);

        when(documentNumberService.nextContractNo(2026)).thenReturn("HD-2026-000100");
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            c.setId(20L);
            return c;
        });
        when(bedRepository.occupyBed(3L, 20L)).thenReturn(1);

        // Configure ratio to 0.5 (50%)
        when(systemConfigService.getBigDecimal(eq("contract.deposit.ratio"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("0.5"));

        Contract contract = contractService.createDraftFromAllocation(app, bed, start, end);

        assertNotNull(contract);
        assertEquals(new BigDecimal("1500000"), contract.getRoomFee());
        // 50% of 1.500.000 = 750.000 VND
        assertEquals(new BigDecimal("750000"), contract.getDepositAmount());
        verify(bedRepository).occupyBed(3L, 20L);
    }
}
