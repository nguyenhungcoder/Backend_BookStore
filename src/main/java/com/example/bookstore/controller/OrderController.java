package com.example.bookstore.controller;

import com.example.bookstore.dto.AdminOrderResponse;
import com.example.bookstore.dto.CreateOrderRequest;
import com.example.bookstore.dto.OrderResponse;
import com.example.bookstore.dto.UpdateOrderStatusRequest;
import com.example.bookstore.security.UserPrincipal;
import com.example.bookstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN".equals(authority.getAuthority()));
        return ResponseEntity.ok(orderService.getOrderById(id, principal.getUserId(), isAdmin));
    }

    // Admin endpoints
    // @GetMapping
    // @PreAuthorize("hasAuthority('ADMIN')")
    // public ResponseEntity<Page<AdminOrderResponse>> getAllOrders(
    //         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    // ) {
    //     Page<AdminOrderResponse> response = orderService.getAllOrders(pageable);
    //     return ResponseEntity.ok(response);
    // }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<AdminOrderResponse>> getAllOrders(
            @RequestParam(required = false) String keyword, // ❤️ Bổ sung dòng này để nhận chữ tìm kiếm
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // ❤️ Truyền keyword xuống cho Service xử lý
        Page<AdminOrderResponse> response = orderService.getAllOrders(keyword, pageable);
        return ResponseEntity.ok(response);
    }
    

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AdminOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        AdminOrderResponse response = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(authority -> "ADMIN".equals(authority.getAuthority()));
        OrderResponse response = orderService.cancelOrder(id, principal.getUserId(), isAdmin);
        return ResponseEntity.ok(response);
    }
}
