package com.example.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuthorRequest {

    @NotBlank(message = "Author name is required")
    @Size(max = 150, message = "Author name must be at most 150 characters")
    private String name;

    @Size(max = 4000, message = "Biography must be at most 4000 characters")
    private String bio;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatar;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
}
