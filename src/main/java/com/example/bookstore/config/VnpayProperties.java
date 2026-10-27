package com.example.bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vnpay")
public record VnpayProperties(
        String payUrl,
        String returnUrl,
        String tmnCode,
        String hashSecret,
        String version,
        String command,
        String orderType
) {
}
