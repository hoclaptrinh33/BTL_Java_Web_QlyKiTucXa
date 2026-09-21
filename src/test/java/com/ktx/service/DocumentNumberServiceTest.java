package com.ktx.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.DocumentSequence;
import com.ktx.repository.DocumentSequenceRepository;
import com.ktx.service.impl.DocumentNumberServiceImpl;

@ExtendWith(MockitoExtension.class)
class DocumentNumberServiceTest {

    @Mock
    private DocumentSequenceRepository sequenceRepository;

    private DocumentNumberService documentNumberService;

    @BeforeEach
    void setUp() {
        documentNumberService = new DocumentNumberServiceImpl(sequenceRepository);
    }

    @Test
    @DisplayName("nextContractNo sinh mã HD-YYYY-000123 khi đã có sequence")
    void nextContractNo_existingSequence() {
        DocumentSequence seq = new DocumentSequence();
        seq.setKind("CONTRACT_NO");
        seq.setYear(2026);
        seq.setLastValue(122);

        when(sequenceRepository.findByKindAndYearForUpdate("CONTRACT_NO", 2026))
                .thenReturn(Optional.of(seq));

        String contractNo = documentNumberService.nextContractNo(2026);

        assertEquals("HD-2026-000123", contractNo);
        assertEquals(123, seq.getLastValue());
        verify(sequenceRepository).save(seq);
    }

    @Test
    @DisplayName("nextContractNo tự tạo sequence mới nếu chưa có trong DB")
    void nextContractNo_newSequence() {
        when(sequenceRepository.findByKindAndYearForUpdate("CONTRACT_NO", 2026))
                .thenReturn(Optional.empty());

        when(sequenceRepository.saveAndFlush(any(DocumentSequence.class))).thenAnswer(invocation -> {
            DocumentSequence s = invocation.getArgument(0);
            return s;
        });

        String contractNo = documentNumberService.nextContractNo(2026);

        assertNotNull(contractNo);
        assertEquals("HD-2026-000001", contractNo);
    }

    @Test
    @DisplayName("nextInvoiceNo sinh mã INV-YYYY-000001")
    void nextInvoiceNo_success() {
        DocumentSequence seq = new DocumentSequence();
        seq.setKind("INVOICE_NO");
        seq.setYear(2026);
        seq.setLastValue(99);

        when(sequenceRepository.findByKindAndYearForUpdate("INVOICE_NO", 2026))
                .thenReturn(Optional.of(seq));

        String invoiceNo = documentNumberService.nextInvoiceNo(2026);

        assertEquals("INV-2026-000100", invoiceNo);
        assertEquals(100, seq.getLastValue());
        verify(sequenceRepository).save(seq);
    }
}
