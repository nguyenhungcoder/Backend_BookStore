package com.example.bookstore.dto;

public class CreatePaymentResponse {
    private String paymentUrl;
    private String orderId;

    public CreatePaymentResponse() {
    }

    public CreatePaymentResponse(String paymentUrl, String orderId) {
        this.paymentUrl = paymentUrl;
        this.orderId = orderId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
