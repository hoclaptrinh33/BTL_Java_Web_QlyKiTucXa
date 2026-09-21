package com.ktx.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.domain.SystemConfig;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.TicketService;

@ExtendWith(MockitoExtension.class)
class TicketAutoCloseJobTest {

    @Mock
    private TicketService ticketService;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    private TicketAutoCloseJob job;

    @BeforeEach
    void setUp() {
        job = new TicketAutoCloseJob(ticketService, systemConfigRepository);
    }

    @Test
    void runAutoClose_readsConfigAndCallsTicketService() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("ticket.autoclose.days");
        config.setConfigValue("7");

        when(systemConfigRepository.findById("ticket.autoclose.days")).thenReturn(Optional.of(config));
        when(ticketService.autoCloseResolvedTickets(7)).thenReturn(3);

        int result = job.runAutoClose();

        assertEquals(3, result);
        verify(ticketService, times(1)).autoCloseResolvedTickets(7);
    }

    @Test
    void runAutoClose_fallbackTo7Days_whenConfigMissing() {
        when(systemConfigRepository.findById("ticket.autoclose.days")).thenReturn(Optional.empty());
        when(ticketService.autoCloseResolvedTickets(7)).thenReturn(0);

        int result = job.runAutoClose();

        assertEquals(0, result);
        verify(ticketService, times(1)).autoCloseResolvedTickets(7);
    }
}
