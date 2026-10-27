package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherResponse {
    private Long publisherId;
    private String name;
    private String address;
    private String phone;
    private String website;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
