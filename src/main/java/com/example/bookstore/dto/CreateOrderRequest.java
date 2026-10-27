package com.example.bookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotNull
    private Long addressId;

    private String voucherCode;

    @DecimalMin("0.00")
    private BigDecimal shippingFee;

    private String paymentMethod;
}
