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
public class AdminContactResponse {

    private Long contactId;

    private String fullName;

    private String email;

    private String phone;

    private String subject;

    private String message;

    private String status;

    private LocalDateTime createdAt;
}
