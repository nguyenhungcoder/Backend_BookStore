package com.example.bookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookRequest {

    private String title;

    private String description;

    private String coverImage;

    private String isbn;

    private Integer publicationYear;

    private String language;

    private Integer pageCount;

    private Double weight;

    private String dimensions;

    private BigDecimal price;

    @Min(value = 0, message = "Discount percent must be >= 0")
    @Max(value = 100, message = "Discount percent must be <= 100")
    private Integer discountPercent;

    private Integer stockQuantity;

    private Long publisherId;

    private String status;

    private List<Long> categoryIds;

    private List<Long> authorIds;
}
