package com.ktx.web.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ktx.domain.enums.TicketStatus;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.UserRepository;
import com.ktx.security.KtxUserDetailsService;
import com.ktx.security.LoginAttemptService;
import com.ktx.security.LoginFailureHandler;
import com.ktx.security.LoginSuccessHandler;
import com.ktx.security.SecurityConfig;
import com.ktx.security.StaffScope;
import com.ktx.service.TicketService;

@WebMvcTest(controllers = StaffTicketController.class)
@Import({SecurityConfig.class, LoginSuccessHandler.class, LoginFailureHandler.class, KtxUserDetailsService.class})
class StaffTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Test
    void staffCanViewTickets() throws Exception {
        when(ticketService.getTicketsForStaff(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/staff/tickets").with(user("staffA").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanUpdateStatus() throws Exception {
        mockMvc.perform(post("/staff/tickets/1/status")
                        .with(user("staffA").roles("STAFF"))
                        .with(csrf())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/tickets"));

        verify(ticketService).updateStatus(eq(1L), eq(TicketStatus.IN_PROGRESS), any(Authentication.class));
    }

    @Test
    void staffOtherBuilding_throwsForbidden403() throws Exception {
        doThrow(new AccessDeniedException(StaffScope.DENIED_BUILDING))
                .when(ticketService).updateStatus(eq(1L), eq(TicketStatus.IN_PROGRESS), any(Authentication.class));

        mockMvc.perform(post("/staff/tickets/1/status")
                        .with(user("staffA").roles("STAFF"))
                        .with(csrf())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }
}
