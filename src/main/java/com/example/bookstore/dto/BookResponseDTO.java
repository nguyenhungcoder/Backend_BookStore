package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponseDTO {

    private Long bookId;

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

    private Integer discountPercent;

    private Integer stockQuantity;

    private Integer sold;

    private String status;

    private PublisherSummaryDTO publisher;

    private List<AuthorSummaryDTO> authors;

    private List<CategorySummaryDTO> categories;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonProperty("id")
    public Long getId() {
        return this.bookId;
    }
}
