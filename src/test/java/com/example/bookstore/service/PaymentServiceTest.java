package com.example.bookstore.service;

import com.example.bookstore.config.VnpayProperties;
import com.example.bookstore.dto.CreatePaymentUrlRequest;
import com.example.bookstore.dto.PaymentUrlResponse;
import com.example.bookstore.dto.VnpayReturnResponse;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.entity.Payment;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private PaymentService paymentService;

    private VnpayProperties vnpayProperties;

    @BeforeEach
    void setUp() {
        vnpayProperties = new VnpayProperties(
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:8080/api/payments/vnpay-return",
                "DEMO_TMN_CODE",
                "DEMO_HASH_SECRET",
                "2.1.0",
                "pay",
                "other"
        );
        paymentService = new PaymentService(paymentRepository, orderRepository, bookRepository, vnpayProperties);
    }

    @Test
    void createPaymentUrlShouldPersistPendingPaymentAndReturnUrl() throws Exception {
        Order order = Order.builder()
                .orderId(10L)
                .totalAmount(new BigDecimal("150000"))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findByOrderIdAndUserUserId(10L, 1L)).thenReturn(java.util.Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        org.mockito.Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        CreatePaymentUrlRequest createRequest = new CreatePaymentUrlRequest();
        createRequest.setOrderId(10L);

        PaymentUrlResponse response = paymentService.createPaymentUrl(1L, createRequest, request);

        assertTrue(response.getPaymentUrl().contains("vnp_TmnCode=DEMO_TMN_CODE"));
        assertTrue(response.getPaymentUrl().contains("vnp_Amount=15000000"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals("PENDING", savedPayment.getStatus());
        assertEquals("VNPAY", savedPayment.getMethod());
        assertEquals(order, savedPayment.getOrder());
    }

    @Test
    void handleVnpayReturnShouldConfirmPaymentAndOrderWhenChecksumValid() throws Exception {
        Payment payment = Payment.builder()
                .paymentId(1L)
                .amount(new BigDecimal("150000"))
                .method("VNPAY")
                .status("PENDING")
                .transactionCode("txn-123")
                .order(Order.builder().orderId(10L).status("PENDING").build())
                .build();

        when(paymentRepository.findByTransactionCode("txn-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "txn-123");
        params.put("vnp_OrderInfo", "Order 10");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_BankCode", "VNBANK");
        params.put("vnp_PayDate", "20240101123045");
        params.put("vnp_TransactionNo", "1001");

        String secureHash = signVnpay(params, "DEMO_HASH_SECRET");
        params.put("vnp_SecureHash", secureHash);

        VnpayReturnResponse response = paymentService.handleVnpayReturn(params);

        assertEquals("00", response.getResponseCode());
        assertEquals("PAID", response.getPaymentStatus());
        assertEquals("CONFIRMED", response.getOrderStatus());
        assertEquals("PAID", payment.getStatus());
        assertEquals("CONFIRMED", payment.getOrder().getStatus());
    }

    @Test
    void handleVnpayReturnShouldCancelOrderWhenUserCancelsPayment() throws Exception {
        Payment payment = Payment.builder()
                .paymentId(2L)
                .amount(new BigDecimal("150000"))
                .method("VNPAY")
                .status("PENDING")
                .transactionCode("txn-cancel")
                .order(Order.builder().orderId(11L).status("PENDING").build())
                .build();

        when(paymentRepository.findByTransactionCode("txn-cancel")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", "24");
        params.put("vnp_TransactionStatus", "01");
        params.put("vnp_TxnRef", "txn-cancel");
        params.put("vnp_OrderInfo", "Order 11");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_BankCode", "VNBANK");
        params.put("vnp_PayDate", "20240101123045");
        params.put("vnp_TransactionNo", "1003");

        String secureHash = signVnpay(params, "DEMO_HASH_SECRET");
        params.put("vnp_SecureHash", secureHash);

        VnpayReturnResponse response = paymentService.handleVnpayReturn(params);

        assertEquals("24", response.getResponseCode());
        assertEquals("FAILED", response.getPaymentStatus());
        assertEquals("CANCELLED", response.getOrderStatus());
        assertEquals("FAILED", payment.getStatus());
        assertEquals("CANCELLED", payment.getOrder().getStatus());
    }

    @Test
    void handleVnpayReturnShouldIncreaseBookSoldCountWhenPaymentSucceeds() throws Exception {
        Book book = Book.builder()
                .bookId(7L)
                .title("Test Book")
                .sold(0)
                .stockQuantity(10)
                .build();

        OrderDetail orderDetail = OrderDetail.builder()
                .book(book)
                .quantity(2)
                .build();

        Order order = Order.builder()
                .orderId(10L)
                .status("PENDING")
                .orderDetails(List.of(orderDetail))
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .amount(new BigDecimal("300000"))
                .method("VNPAY")
                .status("PENDING")
                .transactionCode("txn-456")
                .order(order)
                .build();

        when(paymentRepository.findByTransactionCode("txn-456")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "txn-456");
        params.put("vnp_OrderInfo", "Order 10");
        params.put("vnp_Amount", "30000000");
        params.put("vnp_BankCode", "VNBANK");
        params.put("vnp_PayDate", "20240101123045");
        params.put("vnp_TransactionNo", "1002");

        String secureHash = signVnpay(params, "DEMO_HASH_SECRET");
        params.put("vnp_SecureHash", secureHash);

        paymentService.handleVnpayReturn(params);

        assertEquals(2, book.getSold());
        verify(bookRepository).save(book);
    }

    private String signVnpay(Map<String, String> params, String secret) throws Exception {
        Map<String, String> sorted = new LinkedHashMap<>();
        params.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()) && !"vnp_SecureHashType".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
        }

        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSha512.init(keySpec);
        byte[] bytes = hmacSha512.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }
}
