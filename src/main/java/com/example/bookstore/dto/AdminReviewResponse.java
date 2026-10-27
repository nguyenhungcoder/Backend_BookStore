package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewResponse {

    private Long reviewId;

    private Long userId;

    private String username;

    private Long bookId;

    private String bookTitle;

    private Integer rating;

    private String comment;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
