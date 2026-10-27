package com.example.bookstore.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfileRequest {

    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    private List<AddressDTO> addresses;
}
