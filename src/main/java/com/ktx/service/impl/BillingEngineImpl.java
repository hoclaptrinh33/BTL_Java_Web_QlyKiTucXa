package com.ktx.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.service.BillingEngine;
import com.ktx.service.DocumentNumberService;

@Service
public class BillingEngineImpl implements BillingEngine {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final DocumentNumberService documentNumberService;

    public BillingEngineImpl(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository,
                             DocumentNumberService documentNumberService) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.documentNumberService = documentNumberService;
    }

    @Override
    public long tieredElectricity(int kwh) {
        // Stub - sẽ được hoàn thiện trong PR-12
        return 0L;
    }

    @Override
    public List<Invoice> issueUtilityInvoices(long roomId, YearMonth month) {
        // Stub - sẽ được hoàn thiện trong PR-12
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public Invoice issueRoomFee(long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        String idempotencyKey = "INV:ROOM_TERM:" + contractId;
        Optional<Invoice> existing = invoiceRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal roomFee = contract.getRoomFee() != null ? contract.getRoomFee() : BigDecimal.ZERO;
        int year = contract.getStartDate() != null ? contract.getStartDate().getYear() : LocalDate.now().getYear();
        String invoiceNo = documentNumberService.nextInvoiceNo(year);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(invoiceNo);
        invoice.setStudent(contract.getStudent());
        if (contract.getBed() != null && contract.getBed().getRoom() != null) {
            invoice.setRoom(contract.getBed().getRoom());
        }
        invoice.setContract(contract);
        invoice.setInvoiceType(InvoiceType.ROOM_TERM);
        invoice.setBillingMonth(contract.getStartDate());
        invoice.setSubtotal(roomFee);
        invoice.setLateFee(BigDecimal.ZERO);
        invoice.setTotal(roomFee);
        invoice.setDueDate(contract.getStartDate() != null ? contract.getStartDate().plusDays(7) : LocalDate.now().plusDays(7));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIdempotencyKey(idempotencyKey);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(savedInvoice);
        item.setDescription("Tiền phòng học kỳ hợp đồng " + contract.getContractNo());
        item.setItemCode("ROOM_TERM");
        item.setQty(BigDecimal.ONE);
        item.setUnitPrice(roomFee);
        item.setAmount(roomFee);
        invoiceItemRepository.save(item);

        return savedInvoice;
    }

    @Override
    @Transactional
    public Invoice issueDeposit(long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        String idempotencyKey = "INV:DEPOSIT:" + contractId;
        Optional<Invoice> existing = invoiceRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Cọc: 50% giá phòng/kỳ làm tròn HALF_UP (§04-04)
        BigDecimal depositAmount = contract.getDepositAmount();
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal roomFee = contract.getRoomFee() != null ? contract.getRoomFee() : BigDecimal.ZERO;
            depositAmount = roomFee.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP);
        }

        int year = contract.getStartDate() != null ? contract.getStartDate().getYear() : LocalDate.now().getYear();
        String invoiceNo = documentNumberService.nextInvoiceNo(year);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(invoiceNo);
        invoice.setStudent(contract.getStudent());
        if (contract.getBed() != null && contract.getBed().getRoom() != null) {
            invoice.setRoom(contract.getBed().getRoom());
        }
        invoice.setContract(contract);
        invoice.setInvoiceType(InvoiceType.DEPOSIT);
        invoice.setBillingMonth(contract.getStartDate());
        invoice.setSubtotal(depositAmount);
        invoice.setLateFee(BigDecimal.ZERO);
        invoice.setTotal(depositAmount);
        invoice.setDueDate(contract.getStartDate() != null ? contract.getStartDate().plusDays(7) : LocalDate.now().plusDays(7));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIdempotencyKey(idempotencyKey);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(savedInvoice);
        item.setDescription("Tiền đặt cọc hợp đồng " + contract.getContractNo());
        item.setItemCode("DEPOSIT");
        item.setQty(BigDecimal.ONE);
        item.setUnitPrice(depositAmount);
        item.setAmount(depositAmount);
        invoiceItemRepository.save(item);

        return savedInvoice;
    }

    @Override
    public void applyLateFees(LocalDate today) {
        // Stub - sẽ được hoàn thiện trong PR-13
    }
}
