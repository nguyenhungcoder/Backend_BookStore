package com.example.bookstore.controller;

import com.example.bookstore.dto.RevenueByMonthResponse;
import com.example.bookstore.dto.StatsSummaryResponse;
import com.example.bookstore.security.CustomUserDetailsService;
import com.example.bookstore.security.JwtTokenProvider;
import com.example.bookstore.service.StatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatService statService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void summaryEndpointReturnsDashboardStats() throws Exception {
        StatsSummaryResponse response = StatsSummaryResponse.builder()
                .totalRevenue(new BigDecimal("1250000.50"))
                .totalOrders(245L)
                .totalCustomers(180L)
                .build();
        when(statService.getSummary()).thenReturn(response);

        mockMvc.perform(get("/api/stats/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRevenue").value(1250000.50))
                .andExpect(jsonPath("$.totalOrders").value(245))
                .andExpect(jsonPath("$.totalCustomers").value(180));
    }

    @Test
    void revenueByMonthEndpointReturnsMonthlyRevenueList() throws Exception {
        when(statService.getRevenueByMonth()).thenReturn(List.of(
                RevenueByMonthResponse.builder()
                        .month(1)
                        .monthName("January")
                        .revenue(new BigDecimal("1200.00"))
                        .build()
        ));

        mockMvc.perform(get("/api/stats/revenue-by-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].monthName").value("January"))
                .andExpect(jsonPath("$[0].revenue").value(1200.00));
    }

    @Test
    void revenueByDateRangeEndpointReturnsFilteredRevenue() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        when(statService.getRevenueByDateRange(startDate, endDate)).thenReturn(new BigDecimal("250000.00"));

        mockMvc.perform(get("/api/stats/revenue")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(250000.00));
    }
}
