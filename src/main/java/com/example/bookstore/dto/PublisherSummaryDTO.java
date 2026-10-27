package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublisherSummaryDTO {

    private Long publisherId;

    private String name;

    private String address;

    private String phone;

    private String website;
}
