package com.example.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {

    private String receiverName;

    private String phone;

    private String province;

    private String district;

    private String ward;

    private String detailAddress;

    private Boolean isDefault;
}
