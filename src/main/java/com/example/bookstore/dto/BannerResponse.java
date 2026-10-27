package com.example.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {

    private Long bannerId;

    private String title;

    private String image;

    private String link;

    @NotBlank(message = "Status is required")
    private String status;

    private LocalDateTime createdAt;
}
