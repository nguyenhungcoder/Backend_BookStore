package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.bookstore.dto.CategorySummaryDTO;
import com.example.bookstore.dto.AuthorSummaryDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookListItemResponseDTO {

    private Long bookId;

    private String title;

    private String coverImage;

    private String isbn;

    private BigDecimal price;

    private Integer discountPercent;

    private Integer stockQuantity;

    private Integer sold;

    private String status;

    private PublisherSummaryDTO publisher;

    private List<CategorySummaryDTO> categories;

    private String description;

    private Integer publicationYear;

    private List<AuthorSummaryDTO> authors;

    private LocalDateTime createdAt;

    @JsonProperty("id")
    public Long getId() {
        return this.bookId;
    }
}
