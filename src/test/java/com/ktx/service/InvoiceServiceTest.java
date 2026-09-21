package com.ktx.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Invoice;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.PaymentRepository;
import com.ktx.service.impl.InvoiceServiceImpl;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceServiceImpl(invoiceRepository, invoiceItemRepository, paymentRepository);
    }

    @Test
    @DisplayName("cancel: hủy hóa đơn thành công, đổi status=CANCELLED và tombstone idempotencyKey")
    void testCancel_success_tombstonesKey() {
        Invoice invoice = new Invoice();
        invoice.setId(100L);
        invoice.setInvoiceNo("INV-2026-000100");
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIdempotencyKey("UTILITY:5:10:2026-09");
        invoice.setTotal(new BigDecimal("250000"));

        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(100L)).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        invoiceService.cancel(100L, 1L);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        assertEquals("UTILITY:5:10:2026-09:cancelled:100", invoice.getIdempotencyKey());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("cancel: ném lỗi nếu hóa đơn đã bị CANCELLED từ trước")
    void testCancel_alreadyCancelled_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(101L);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(101L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessException.class, () -> invoiceService.cancel(101L, 1L));
    }

    @Test
    @DisplayName("cancel: ném lỗi nếu hóa đơn đã PAID")
    void testCancel_alreadyPaid_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(102L);
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(102L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessException.class, () -> invoiceService.cancel(102L, 1L));
    }

    @Test
    @DisplayName("cancel: ném lỗi nếu hóa đơn đã có khoản thanh toán được ghi nhận")
    void testCancel_hasPayments_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(103L);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotal(new BigDecimal("500000"));

        when(invoiceRepository.findById(103L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(103L)).thenReturn(new BigDecimal("200000"));

        assertThrows(BusinessException.class, () -> invoiceService.cancel(103L, 1L));
    }

    @Test
    @DisplayName("getRemainingAmount: tính đúng số tiền còn lại")
    void testGetRemainingAmount() {
        Invoice invoice = new Invoice();
        invoice.setId(104L);
        invoice.setTotal(new BigDecimal("1000000"));

        when(invoiceRepository.findById(104L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.sumAmountByInvoiceId(104L)).thenReturn(new BigDecimal("400000"));

        BigDecimal remaining = invoiceService.getRemainingAmount(104L);
        assertEquals(new BigDecimal("600000"), remaining);
    }
}
