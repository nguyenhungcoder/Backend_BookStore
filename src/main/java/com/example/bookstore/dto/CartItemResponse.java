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
public class CartItemResponse {

    private Long itemId;

    private Long bookId;

    private String title;

    private String author;

    private String category;

    private String publisher;

    private String coverImage;

    private BigDecimal price;

    private Integer stockQuantity;

    private Integer quantity;

    private BigDecimal subtotal;

    private LocalDateTime createdAt;
}
