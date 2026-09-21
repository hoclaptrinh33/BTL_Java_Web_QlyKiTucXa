package com.ktx.service;

public interface DocumentNumberService {

    /**
     * Sinh số hợp đồng định dạng: HD-YYYY-000123 (với 6 chữ số tự tăng qua bảng document_sequences)
     */
    String nextContractNo(int year);

    /**
     * Sinh số hóa đơn định dạng: INV-YYYY-000123
     */
    String nextInvoiceNo(int year);
}
