package com.ktx.service;

public interface ExportService {

    byte[] exportResidentsXlsx();

    byte[] exportDebtsXlsx();

    byte[] exportDebtsPdf();

    byte[] exportInvoicePdf(Long invoiceId);
}
