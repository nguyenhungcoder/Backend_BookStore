package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long orderId;

    private Long addressId;

    private String voucherCode;

    private BigDecimal itemsTotal;

    private BigDecimal discountAmount;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    private String paymentMethod;

    private String status;

    private LocalDateTime createdAt;

    private List<OrderDetailResponse> orderDetails;
}
