package com.ktx.scheduler;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.service.BillingEngine;

@ExtendWith(MockitoExtension.class)
class InvoiceOverdueJobTest {

    @Mock
    private BillingEngine billingEngine;

    @InjectMocks
    private InvoiceOverdueJob job;

    @Test
    @DisplayName("scanOverdueInvoices gọi applyLateFees với ngày hiện tại")
    void testScanOverdueInvoices_callsApplyLateFees() {
        job.scanOverdueInvoices();
        verify(billingEngine).applyLateFees(LocalDate.now());
    }
}
