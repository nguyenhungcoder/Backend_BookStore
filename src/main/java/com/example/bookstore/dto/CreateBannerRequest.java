package com.example.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBannerRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Image is required")
    private String image;

    private String link;

    @NotBlank(message = "Status is required")
    private String status;
}
