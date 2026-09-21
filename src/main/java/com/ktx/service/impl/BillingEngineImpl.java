package com.ktx.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.InvoiceItem;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceItemRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.UtilityReadingRepository;
import com.ktx.service.BillingEngine;
import com.ktx.service.DocumentNumberService;

@Service
public class BillingEngineImpl implements BillingEngine {

    public static class ElectricityTier {
        private Integer to;
        private long price;

        public ElectricityTier() {
        }

        public ElectricityTier(Integer to, long price) {
            this.to = to;
            this.price = price;
        }

        public Integer getTo() {
            return to;
        }

        public void setTo(Integer to) {
            this.to = to;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }
    }

    public static final List<ElectricityTier> DEFAULT_TIERS = List.of(
            new ElectricityTier(50, 1984),
            new ElectricityTier(100, 2050),
            new ElectricityTier(200, 2380),
            new ElectricityTier(300, 2998),
            new ElectricityTier(400, 3350),
            new ElectricityTier(null, 3460)
    );

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final DocumentNumberService documentNumberService;
    private final UtilityReadingRepository utilityReadingRepository;
    private final SystemConfigRepository systemConfigRepository;

    public BillingEngineImpl(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository,
                             DocumentNumberService documentNumberService,
                             UtilityReadingRepository utilityReadingRepository,
                             SystemConfigRepository systemConfigRepository) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.documentNumberService = documentNumberService;
        this.utilityReadingRepository = utilityReadingRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    public BillingEngineImpl(ContractRepository contractRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceItemRepository invoiceItemRepository,
                             DocumentNumberService documentNumberService) {
        this(contractRepository, invoiceRepository, invoiceItemRepository, documentNumberService, null, null);
    }

    @Override
    public long tieredElectricity(int kwh) {
        if (kwh <= 0) {
            return 0L;
        }

        List<ElectricityTier> tiers = loadElectricityTiers();
        long total = 0L;
        int remaining = kwh;
        int prevTo = 0;

        for (ElectricityTier tier : tiers) {
            if (remaining <= 0) {
                break;
            }
            if (tier.getTo() != null) {
                int capacity = tier.getTo() - prevTo;
                if (capacity <= 0) {
                    continue;
                }
                int consumed = Math.min(remaining, capacity);
                total += (long) consumed * tier.getPrice();
                remaining -= consumed;
                prevTo = tier.getTo();
            } else {
                total += (long) remaining * tier.getPrice();
                remaining = 0;
                break;
            }
        }
        return total;
    }

    public static List<ElectricityTier> parseTiersJson(String json) {
        if (json == null || json.isBlank()) {
            return DEFAULT_TIERS;
        }
        try {
            List<ElectricityTier> list = new ArrayList<>();
            Matcher matcher = Pattern.compile("\\{[^}]*\\}").matcher(json);
            while (matcher.find()) {
                String obj = matcher.group();
                Integer to = null;
                long price = 0;

                Matcher toMatcher = Pattern.compile("\"to\"\\s*:\\s*(\\d+|null)").matcher(obj);
                if (toMatcher.find()) {
                    String toVal = toMatcher.group(1);
                    if (!"null".equalsIgnoreCase(toVal)) {
                        to = Integer.parseInt(toVal);
                    }
                }

                Matcher priceMatcher = Pattern.compile("\"price\"\\s*:\\s*(\\d+)").matcher(obj);
                if (priceMatcher.find()) {
                    price = Long.parseLong(priceMatcher.group(1));
                }

                list.add(new ElectricityTier(to, price));
            }
            return list.isEmpty() ? DEFAULT_TIERS : list;
        } catch (Exception e) {
            return DEFAULT_TIERS;
        }
    }

    private List<ElectricityTier> loadElectricityTiers() {
        if (systemConfigRepository == null) {
            return DEFAULT_TIERS;
        }
        try {
            return systemConfigRepository.findById("billing.electricity.tiers")
                    .map(SystemConfig::getConfigValue)
                    .filter(val -> val != null && !val.isBlank())
                    .map(BillingEngineImpl::parseTiersJson)
                    .orElse(DEFAULT_TIERS);
        } catch (Exception e) {
            return DEFAULT_TIERS;
        }
    }

    @Override
    @Transactional
    public List<Invoice> issueUtilityInvoices(long roomId, YearMonth month) {
        if (utilityReadingRepository == null) {
            throw new BusinessException("UtilityReadingRepository chưa được cấu hình");
        }

        LocalDate billingMonth = month.atDay(1);
        UtilityReading reading = utilityReadingRepository.findByRoomIdAndBillingMonth(roomId, billingMonth)
                .orElseThrow(() -> new BusinessException("Chưa có chỉ số điện nước tháng " + month + " cho phòng #" + roomId));

        int kwh = reading.calculateKwh();
        int m3 = reading.calculateM3();

        if (kwh < 0 || m3 < 0) {
            throw new BusinessException("Từ chối lập hóa đơn nếu kWh hoặc m3 null / âm");
        }

        long elecTotal = tieredElectricity(kwh);
        long waterPrice = getConfigLong("billing.water.price_per_m3", 15000L);
        long waterTotal = (long) m3 * waterPrice;
        long internetPerRoom = getConfigLong("billing.fee.internet_per_room", 50000L);
        long sanitationPerPerson = getConfigLong("billing.fee.sanitation_per_person", 20000L);
        long parkingPerPerson = getConfigLong("billing.fee.parking_per_person", 30000L);
        int dueDays = getConfigInt("billing.due.days", 10);

        long divisible = elecTotal + waterTotal + internetPerRoom;

        List<Contract> contracts = new ArrayList<>(contractRepository.findOccupyingByRoomId(roomId, OccupyingStatuses.OCCUPYING));
        int N = contracts.size();
        if (N == 0) {
            return Collections.emptyList();
        }

        // Sắp xếp student_id ASC để tìm min(student_id)
        contracts.sort(Comparator.comparing(c -> c.getStudent().getId()));
        Long minStudentId = contracts.get(0).getStudent().getId();

        long share = divisible / N; // Nguyên (floor)
        long residual = divisible - (share * N); // Luôn 0..N-1

        String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int year = month.getYear();
        List<Invoice> resultInvoices = new ArrayList<>();

        for (Contract contract : contracts) {
            Student student = contract.getStudent();
            Long studentId = student.getId();

            long roomPart = share + (studentId.equals(minStudentId) ? residual : 0);
            long subtotal = roomPart + sanitationPerPerson + parkingPerPerson;
            long lateFee = 0;
            long total = subtotal + lateFee;

            String idempotencyKey = String.format("UTILITY:%d:%d:%s", studentId, roomId, monthStr);
            Optional<Invoice> existingOpt = invoiceRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                Invoice existing = existingOpt.get();
                if (existing.getStatus() != InvoiceStatus.CANCELLED) {
                    resultInvoices.add(existing);
                    continue;
                }
            }

            String invoiceNo = documentNumberService.nextInvoiceNo(year);
            Invoice invoice = new Invoice();
            invoice.setInvoiceNo(invoiceNo);
            invoice.setStudent(student);
            invoice.setRoom(reading.getRoom());
            invoice.setContract(contract);
            invoice.setInvoiceType(InvoiceType.UTILITY);
            invoice.setBillingMonth(billingMonth);
            invoice.setSubtotal(BigDecimal.valueOf(subtotal));
            invoice.setLateFee(BigDecimal.ZERO);
            invoice.setTotal(BigDecimal.valueOf(total));
            invoice.setDueDate(LocalDate.now().plusDays(dueDays));
            invoice.setStatus(InvoiceStatus.UNPAID);
            invoice.setIdempotencyKey(idempotencyKey);

            Invoice savedInvoice = invoiceRepository.save(invoice);

            long elecLine;
            long waterLine;
            long inetLine;
            if (divisible == 0) {
                elecLine = 0;
                waterLine = 0;
                inetLine = 0;
            } else {
                elecLine = (elecTotal * roomPart) / divisible;
                waterLine = (waterTotal * roomPart) / divisible;
                inetLine = roomPart - elecLine - waterLine;
            }

            createInvoiceItem(savedInvoice, "Tiền điện (" + kwh + " kWh) tháng " + monthStr, "ELEC", BigDecimal.valueOf(elecLine));
            createInvoiceItem(savedInvoice, "Tiền nước (" + m3 + " m³) tháng " + monthStr, "WATER", BigDecimal.valueOf(waterLine));
            createInvoiceItem(savedInvoice, "Phí internet phòng tháng " + monthStr, "INTERNET", BigDecimal.valueOf(inetLine));
            createInvoiceItem(savedInvoice, "Phí vệ sinh tháng " + monthStr, "SANITATION", BigDecimal.valueOf(sanitationPerPerson));
            createInvoiceItem(savedInvoice, "Phí gửi xe tháng " + monthStr, "PARKING", BigDecimal.valueOf(parkingPerPerson));

            resultInvoices.add(savedInvoice);
        }

        return resultInvoices;
    }

    private void createInvoiceItem(Invoice invoice, String desc, String code, BigDecimal amount) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(desc);
        item.setItemCode(code);
        item.setQty(BigDecimal.ONE);
        item.setUnitPrice(amount);
        item.setAmount(amount);
        invoiceItemRepository.save(item);
    }

    private long getConfigLong(String key, long defaultValue) {
        if (systemConfigRepository == null) {
            return defaultValue;
        }
        try {
            return systemConfigRepository.findById(key)
                    .map(SystemConfig::getConfigValue)
                    .filter(val -> val != null && !val.isBlank())
                    .map(String::trim)
                    .map(Long::parseLong)
                    .orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        return (int) getConfigLong(key, defaultValue);
    }

    @Override
    @Transactional
    public Invoice issueRoomFee(long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy hợp đồng #" + contractId));

        String idempotencyKey = "INV:ROOM_TERM:" + contractId;
        Optional<Invoice> existing = invoiceRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Invoice inv = existing.get();
            if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                return inv;
            }
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
            Invoice inv = existing.get();
            if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                return inv;
            }
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
    @Transactional
    public void applyLateFees(LocalDate today) {
        BigDecimal lateRate = getConfigBigDecimal("billing.late.rate", new BigDecimal("0.05"));

        List<Invoice> candidates = invoiceRepository.findByStatusInAndDueDateBefore(
                List.of(InvoiceStatus.UNPAID, InvoiceStatus.OVERDUE), today);

        for (Invoice invoice : candidates) {
            if (invoice.getStatus() == InvoiceStatus.UNPAID) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
            }

            // Phí trễ áp tối đa 1 lần: nếu late_fee > 0 rồi thì no-op
            if (invoice.getLateFee() == null || invoice.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal subtotal = invoice.getSubtotal();
                if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lateFee = subtotal.multiply(lateRate).setScale(0, RoundingMode.CEILING);
                    invoice.setLateFee(lateFee);
                    invoice.setTotal(subtotal.add(lateFee));
                    createInvoiceItem(invoice, "Phí phạt chậm nộp", "LATE_FEE", lateFee);
                }
            }
            invoiceRepository.save(invoice);
        }
    }

    private BigDecimal getConfigBigDecimal(String key, BigDecimal defaultValue) {
        if (systemConfigRepository == null) {
            return defaultValue;
        }
        try {
            return systemConfigRepository.findById(key)
                    .map(SystemConfig::getConfigValue)
                    .filter(val -> val != null && !val.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
