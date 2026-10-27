package com.example.bookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateContactRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Phone number must be valid")
    private String phone;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
