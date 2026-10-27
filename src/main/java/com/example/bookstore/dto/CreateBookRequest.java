package com.example.bookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class CreateBookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String coverImage;

    @NotBlank(message = "ISBN is required")
    private String isbn;

    @NotNull(message = "Publication year is required")
    private Integer publicationYear;

    private String language;

    private Integer pageCount;

    private Double weight;

    private String dimensions;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Discount percent is required")
    @Min(value = 0, message = "Discount percent must be >= 0")
    @Max(value = 100, message = "Discount percent must be <= 100")
    private Integer discountPercent;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity must be >= 0")
    private Integer stockQuantity;

    @NotNull(message = "Publisher ID is required")
    private Long publisherId;

    @NotNull(message = "Status is required")
    private String status;

    @NotNull(message = "Category IDs are required")
    private List<Long> categoryIds;

    @NotNull(message = "Author IDs are required")
    private List<Long> authorIds;
}
