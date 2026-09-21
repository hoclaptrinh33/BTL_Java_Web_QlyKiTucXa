package com.ktx.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Invoice;
import com.ktx.domain.Payment;
import com.ktx.domain.User;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.PaymentMethod;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.PaymentRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              InvoiceRepository invoiceRepository,
                              UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method, String referenceNo, Long actorId) {
        if (invoiceId == null) {
            throw new BusinessException("ID hóa đơn không hợp lệ");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền thanh toán phải lớn hơn 0");
        }
        if (method == null) {
            method = PaymentMethod.CASH;
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn #" + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Không thể thanh toán hóa đơn đã bị hủy #" + invoice.getInvoiceNo());
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Hóa đơn #" + invoice.getInvoiceNo() + " đã được thanh toán đầy đủ");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin người dùng thực hiện #" + actorId));

        BigDecimal currentPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        BigDecimal remaining = invoice.getTotal().subtract(currentPaid != null ? currentPaid : BigDecimal.ZERO);

        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException(String.format("Số tiền nộp (%s đ) vượt quá số tiền còn nợ (%s đ) của hóa đơn %s",
                    amount.toPlainString(), remaining.toPlainString(), invoice.getInvoiceNo()));
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRecordedBy(actor);
        payment.setReferenceNo(referenceNo != null && !referenceNo.isBlank() ? referenceNo.trim() : null);

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newTotalPaid = (currentPaid != null ? currentPaid : BigDecimal.ZERO).add(amount);
        if (newTotalPaid.compareTo(invoice.getTotal()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(savedPayment.getPaidAt());
            invoiceRepository.save(invoice);
        }

        return savedPayment;
    }

    @Override
    public List<Payment> searchPayments(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return paymentRepository.searchPayments(keyword.trim());
        }
        return paymentRepository.findAllByOrderByPaidAtDesc();
    }

    @Override
    public List<Payment> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoiceId);
    }
}
