package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatsSummaryResponse {

    private BigDecimal totalRevenue;

    private Long totalOrders;

    private Long totalCustomers;
}
