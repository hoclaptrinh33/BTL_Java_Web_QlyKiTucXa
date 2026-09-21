package com.ktx.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.ktx.domain.Payment;
import com.ktx.domain.User;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.PaymentMethod;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.PaymentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.impl.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentService paymentService;

    private User staffUser;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, invoiceRepository, userRepository);
        staffUser = new User();
        staffUser.setId(1L);
        staffUser.setUsername("admin");
    }

    @Test
    @DisplayName("recordPayment: trả góp đợt 1 (chưa đủ), trạng thái hóa đơn vẫn là UNPAID")
    void testRecordPayment_partialPayment_remainsUnpaid() {
        Invoice invoice = new Invoice();
        invoice.setId(200L);
        invoice.setInvoiceNo("INV-2026-000200");
        invoice.setTotal(new BigDecimal("1000000"));
        invoice.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findById(200L)).thenReturn(Optional.of(invoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffUser));
        when(paymentRepository.sumAmountByInvoiceId(200L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(p -> p.getArgument(0));

        Payment payment = paymentService.recordPayment(200L, new BigDecimal("400000"), PaymentMethod.CASH, "REC-001", 1L);

        assertNotNull(payment);
        assertEquals(new BigDecimal("400000"), payment.getAmount());
        assertEquals(PaymentMethod.CASH, payment.getMethod());
        assertEquals(InvoiceStatus.UNPAID, invoice.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("recordPayment: trả góp đợt 2 đủ 100%, trạng thái hóa đơn chuyển thành PAID")
    void testRecordPayment_finalPayment_becomesPaid() {
        Invoice invoice = new Invoice();
        invoice.setId(201L);
        invoice.setInvoiceNo("INV-2026-000201");
        invoice.setTotal(new BigDecimal("1000000"));
        invoice.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findById(201L)).thenReturn(Optional.of(invoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffUser));
        when(paymentRepository.sumAmountByInvoiceId(201L)).thenReturn(new BigDecimal("400000"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(p -> p.getArgument(0));

        Payment payment = paymentService.recordPayment(201L, new BigDecimal("600000"), PaymentMethod.BANK_TRANSFER, "TXN-999", 1L);

        assertNotNull(payment);
        assertEquals(new BigDecimal("600000"), payment.getAmount());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertNotNull(invoice.getPaidAt());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    @DisplayName("recordPayment: ném lỗi nếu nộp vượt quá số tiền còn lại")
    void testRecordPayment_overpayment_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(202L);
        invoice.setInvoiceNo("INV-2026-000202");
        invoice.setTotal(new BigDecimal("1000000"));
        invoice.setStatus(InvoiceStatus.UNPAID);

        when(invoiceRepository.findById(202L)).thenReturn(Optional.of(invoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffUser));
        when(paymentRepository.sumAmountByInvoiceId(202L)).thenReturn(new BigDecimal("800000"));

        // Còn nợ 200.000, nộp 300.000 -> throw
        assertThrows(BusinessException.class, () ->
                paymentService.recordPayment(202L, new BigDecimal("300000"), PaymentMethod.CASH, null, 1L));
    }

    @Test
    @DisplayName("recordPayment: ném lỗi khi nộp hóa đơn đã PAID")
    void testRecordPayment_alreadyPaid_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(203L);
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(203L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessException.class, () ->
                paymentService.recordPayment(203L, new BigDecimal("100000"), PaymentMethod.CASH, null, 1L));
    }

    @Test
    @DisplayName("recordPayment: ném lỗi khi nộp hóa đơn đã CANCELLED")
    void testRecordPayment_cancelled_throwsException() {
        Invoice invoice = new Invoice();
        invoice.setId(204L);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(204L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessException.class, () ->
                paymentService.recordPayment(204L, new BigDecimal("100000"), PaymentMethod.CASH, null, 1L));
    }

    @Test
    @DisplayName("recordPayment: ném lỗi nếu số tiền <= 0")
    void testRecordPayment_invalidAmount_throwsException() {
        assertThrows(BusinessException.class, () ->
                paymentService.recordPayment(205L, BigDecimal.ZERO, PaymentMethod.CASH, null, 1L));
        assertThrows(BusinessException.class, () ->
                paymentService.recordPayment(205L, new BigDecimal("-50000"), PaymentMethod.CASH, null, 1L));
    }
}
