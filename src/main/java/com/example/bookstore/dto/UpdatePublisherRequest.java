package com.example.bookstore.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePublisherRequest {

    @Size(max = 150, message = "Publisher name must be at most 150 characters")
    private String name;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Pattern(regexp = "^[0-9+().\\s-]{0,20}$", message = "Phone number format is invalid")
    private String phone;

    @Size(max = 255, message = "Website must be at most 255 characters")
    private String website;

    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;
}
