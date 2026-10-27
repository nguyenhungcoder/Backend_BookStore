package com.example.bookstore.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @Size(max = 150, message = "Category name must be at most 150 characters")
    private String name;

    @Size(max = 180, message = "Slug must be at most 180 characters")
    private String slug;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
}
