package com.example.bookstore.security;

import java.security.MessageDigest;

final class MessageDigestHelper {

    private MessageDigestHelper() {
    }

    static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(expected, actual);
    }
}
