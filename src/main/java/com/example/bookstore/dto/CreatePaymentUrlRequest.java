package com.example.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentUrlRequest {

    @NotNull
    private Long orderId;
}
