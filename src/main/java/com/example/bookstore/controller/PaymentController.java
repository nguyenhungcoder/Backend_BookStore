package com.example.bookstore.controller;

import com.example.bookstore.dto.CreatePaymentUrlRequest;
import com.example.bookstore.dto.PaymentUrlResponse;
import com.example.bookstore.security.UserPrincipal;
import com.example.bookstore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(originPatterns = "*") // Allow origin patterns (compatible with allowCredentials)
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<PaymentUrlResponse> createPaymentUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePaymentUrlRequest request,
            HttpServletRequest httpRequest
    ) {
        PaymentUrlResponse response = paymentService.createPaymentUrl(principal.getUserId(), request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 🔥 ĐÃ SỬA: Hàm này giờ sẽ trả về RedirectView để bế thẳng người dùng về React
    @GetMapping({"/vnpay-return", "/vnpay/return"})
    public RedirectView vnpayReturn(HttpServletRequest request) {
        // 1. Chạy logic xử lý chữ ký và cập nhật Database
        paymentService.handleVnpayReturn(request);

        // 2. Lấy các thông số cần thiết để gửi cho React
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef");
        String amount = request.getParameter("vnp_Amount");

        // 3. Chuyển hướng (Redirect) người dùng về thẳng giao diện React
        // LƯU Ý: Nếu React của em dùng Vite chạy cổng 5173 thì sửa 3000 thành 5173 nhé!
        String reactUrl = "http://localhost:3000/vnpay-return"
                + "?vnp_ResponseCode=" + (responseCode != null ? responseCode : "")
                + "&vnp_TxnRef=" + (txnRef != null ? txnRef : "")
                + "&vnp_Amount=" + (amount != null ? amount : "");

        return new RedirectView(reactUrl);
    }

    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        Map<String, String> response = paymentService.handleVnpayIpn(request);
        return ResponseEntity.ok(response);
    }
}