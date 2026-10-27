package com.example.bookstore.service;

import com.example.bookstore.config.VnpayProperties;
import com.example.bookstore.dto.CreatePaymentUrlRequest;
import com.example.bookstore.dto.PaymentUrlResponse;
import com.example.bookstore.dto.VnpayReturnResponse;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.entity.Payment;
import com.example.bookstore.exception.PaymentInvalidException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final VnpayProperties vnpayProperties;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, BookRepository bookRepository, VnpayProperties vnpayProperties) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.vnpayProperties = vnpayProperties;
    }

    @Transactional
    public PaymentUrlResponse createPaymentUrl(Long userId, CreatePaymentUrlRequest request, HttpServletRequest httpRequest) {
        Order order = orderRepository.findByOrderIdAndUserUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new PaymentInvalidException("Order is not pending for payment");
        }

        String txnRef = "txn-" + System.currentTimeMillis();
        String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String amountInMinorUnit = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", vnpayProperties.version());
        params.put("vnp_Command", vnpayProperties.command());
        params.put("vnp_TmnCode", vnpayProperties.tmnCode());
        params.put("vnp_Amount", amountInMinorUnit);
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Order " + order.getOrderId());
        params.put("vnp_OrderType", vnpayProperties.orderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.returnUrl());
        params.put("vnp_IpAddr", httpRequest.getRemoteAddr());
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String paymentUrl = buildPaymentUrl(params);

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method("VNPAY")
                .transactionCode(txnRef)
                .status("PENDING")
                .paidAt(null)
                .build();
        paymentRepository.save(payment);

        return PaymentUrlResponse.builder().paymentUrl(paymentUrl).build();
    }

    @Transactional
    public VnpayReturnResponse handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        return processVnpayReturn(params);
    }

    // Backwards-compatible overload used by unit tests and other callers
    @Transactional
    public VnpayReturnResponse handleVnpayReturn(Map<String, String> params) {
        return processVnpayReturn(params);
    }

    private VnpayReturnResponse processVnpayReturn(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            throw new PaymentInvalidException("Missing VNPay secure hash");
        }

        if (!isValidSignature(params)) {
            throw new PaymentInvalidException("Invalid VNPay signature");
        }

        String transactionCode = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        payment.setStatus(success ? "PAID" : "FAILED");
        payment.setTransactionCode(transactionCode);
        payment.setPaidAt(success ? LocalDateTime.now() : null);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(success ? "CONFIRMED" : "CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (success) {
            updateBookSalesStats(order);
        }

        return VnpayReturnResponse.builder()
                .responseCode(responseCode)
                .message(success ? "Payment successful" : "Payment failed")
                .orderId(order.getOrderId())
                .paymentStatus(payment.getStatus())
                .orderStatus(order.getStatus())
                .build();
    }

    private void updateBookSalesStats(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }

        for (OrderDetail orderDetail : order.getOrderDetails()) {
            if (orderDetail == null || orderDetail.getBook() == null) {
                continue;
            }

            Book book = orderDetail.getBook();
            int quantity = orderDetail.getQuantity() == null ? 0 : orderDetail.getQuantity();
            if (quantity <= 0) {
                continue;
            }

            int currentSold = book.getSold() == null ? 0 : book.getSold();
            book.setSold(currentSold + quantity);
            bookRepository.save(book);
        }
    }

    private boolean isValidSignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        String secureHashType = params.get("vnp_SecureHashType");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }
        if (secureHashType != null && !secureHashType.isBlank() && !"HmacSHA512".equalsIgnoreCase(secureHashType)) {
            return false;
        }
        Map<String, String> sortedParams = new LinkedHashMap<>();
        params.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()) && !"vnp_SecureHashType".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedParams.put(entry.getKey(), entry.getValue()));

        // Build encoded data string (URL-encode values using US-ASCII) per VNPay spec
        StringBuilder dataToHashEncoded = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (!first) {
                    dataToHashEncoded.append('&');
                }
                String encodedValue;
                try {
                    encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString());
                } catch (java.io.UnsupportedEncodingException e) {
                    throw new PaymentInvalidException("Unable to encode VNPay parameter");
                }
                dataToHashEncoded.append(entry.getKey()).append('=').append(encodedValue);
                first = false;
            }
        }

        // Try encoded variant first (recommended), fall back to raw variant for compatibility
        String encodedHash = hmacSha512(dataToHashEncoded.toString());
        if (encodedHash.equalsIgnoreCase(secureHash)) {
            return true;
        }

        // Fallback: raw values concatenation (legacy behavior / unit tests)
        StringBuilder dataToHashRaw = new StringBuilder();
        first = true;
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (!first) {
                    dataToHashRaw.append('&');
                }
                dataToHashRaw.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
        }
        String rawHash = hmacSha512(dataToHashRaw.toString());
        return rawHash.equalsIgnoreCase(secureHash);
    }

    private String buildPaymentUrl(Map<String, String> params) {
        String queryString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));

        String secureHash = hmacSha512(queryString);
        return vnpayProperties.payUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(vnpayProperties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new PaymentInvalidException("Unable to generate VNPay signature");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @org.springframework.transaction.annotation.Transactional
    public java.util.Map<String, String> handleVnpayIpn(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        java.util.Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        return processVnpayIpn(params);

    }

    // Backwards-compatible overload for IPN
    @org.springframework.transaction.annotation.Transactional
    public java.util.Map<String, String> handleVnpayIpn(java.util.Map<String, String> params) {
        return processVnpayIpn(params);
    }

    private java.util.Map<String, String> processVnpayIpn(java.util.Map<String, String> params) {
        java.util.Map<String, String> resp = new java.util.HashMap<>();

        // 1. Validate signature
        if (!isValidSignature(params)) {
            resp.put("RspCode", "97");
            resp.put("Message", "Invalid signature");
            return resp;
        }

        // 2. Get txnRef and responseCode
        String transactionCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        // 3. Find payment by transaction code -> then get order
        java.util.Optional<Payment> paymentOpt = paymentRepository.findByTransactionCode(transactionCode);
        if (paymentOpt.isEmpty()) {
            resp.put("RspCode", "01");
            resp.put("Message", "Order not found");
            return resp;
        }

        Payment payment = paymentOpt.get();
        Order order = payment.getOrder();

        // 4. If already PAID (or order already confirmed), return already confirmed
        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            resp.put("RspCode", "02");
            resp.put("Message", "Order already confirmed");
            return resp;
        }

        // 5. Update statuses based on vnp_ResponseCode
        boolean success = "00".equals(responseCode);
        payment.setStatus(success ? "PAID" : "FAILED");
        payment.setTransactionCode(transactionCode);
        payment.setPaidAt(success ? java.time.LocalDateTime.now() : null);
        paymentRepository.save(payment);

        order.setStatus(success ? "CONFIRMED" : "CANCELLED");
        order.setUpdatedAt(java.time.LocalDateTime.now());
        orderRepository.save(order);

        if (success) {
            updateBookSalesStats(order);
        }

        // 6. Return success response to VNPay
        resp.put("RspCode", "00");
        resp.put("Message", "Confirm Success");
        return resp;
    }
}
