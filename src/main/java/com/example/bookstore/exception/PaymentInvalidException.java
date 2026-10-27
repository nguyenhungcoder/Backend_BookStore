package com.example.bookstore.exception;

public class PaymentInvalidException extends RuntimeException {

    public PaymentInvalidException(String message) {
        super(message);
    }
}
