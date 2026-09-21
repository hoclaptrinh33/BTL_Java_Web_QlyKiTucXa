package com.ktx.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebtByMonthDto {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private List<String> labels = new ArrayList<>();
    private List<BigDecimal> data = new ArrayList<>();
    private List<DebtItem> items = new ArrayList<>();
    private BigDecimal totalDebt = BigDecimal.ZERO;
    private String formattedTotalDebt = "0 đ";
    private long totalInvoices = 0;

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<BigDecimal> getData() {
        return data;
    }

    public void setData(List<BigDecimal> data) {
        this.data = data;
    }

    public List<DebtItem> getItems() {
        return items;
    }

    public void setItems(List<DebtItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt != null ? totalDebt : BigDecimal.ZERO;
        this.formattedTotalDebt = formatVnd(this.totalDebt);
    }

    public String getFormattedTotalDebt() {
        return formattedTotalDebt;
    }

    public void setFormattedTotalDebt(String formattedTotalDebt) {
        this.formattedTotalDebt = formattedTotalDebt;
    }

    public long getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(long totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        return CURRENCY_FORMAT.format(amount.longValue()) + " đ";
    }

    public static class DebtItem {
        private String month;
        private String label;
        private BigDecimal amount = BigDecimal.ZERO;
        private String formattedAmount = "0 đ";
        private long count;

        public DebtItem() {
        }

        public DebtItem(String month, String label, BigDecimal amount, long count) {
            this.month = month;
            this.label = label;
            this.amount = amount != null ? amount : BigDecimal.ZERO;
            this.formattedAmount = DebtByMonthDto.formatVnd(this.amount);
            this.count = count;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount != null ? amount : BigDecimal.ZERO;
            this.formattedAmount = DebtByMonthDto.formatVnd(this.amount);
        }

        public String getFormattedAmount() {
            return formattedAmount;
        }

        public void setFormattedAmount(String formattedAmount) {
            this.formattedAmount = formattedAmount;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
