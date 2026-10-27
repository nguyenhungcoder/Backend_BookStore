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
public class OrderDetailResponse {

    private Long detailId;

    private Long bookId;

    private String title;

    private String coverImage;

    private Integer quantity;

    private String paymentMethod;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}
