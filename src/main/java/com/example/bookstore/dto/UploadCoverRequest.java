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
public class UploadCoverRequest {

    @NotBlank(message = "Cover image data is required")
    private String coverImage;

    @NotBlank(message = "File name is required")
    private String fileName;
}
