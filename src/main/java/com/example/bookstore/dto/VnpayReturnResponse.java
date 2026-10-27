package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VnpayReturnResponse {

    private String responseCode;

    private String message;

    private Long orderId;

    private String paymentStatus;

    private String orderStatus;
}
