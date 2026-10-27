package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderResponse {

    private Long orderId;

    private UserResponse user;

    private AddressResponse address;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal shippingFee;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
