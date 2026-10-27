package com.example.bookstore.service;

import com.example.bookstore.dto.OrderResponse;
import com.example.bookstore.entity.Address;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.repository.AddressRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private VoucherService voucherService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getOrderByIdForCustomerReturnsOrder() {
        Long userId = 1L;
        Long orderId = 10L;

        Address address = Address.builder().addressId(2L).build();
        Book book = Book.builder()
                .bookId(3L)
                .title("Clean Code")
                .coverImage("cover.png")
                .price(new BigDecimal("100.00"))
                .build();
        OrderDetail detail = OrderDetail.builder()
                .book(book)
                .quantity(2)
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("200.00"))
                .build();

        Order order = Order.builder()
                .orderId(orderId)
                .address(address)
                .status("PENDING")
                .totalAmount(new BigDecimal("200.00"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .build();
        order.setOrderDetails(List.of(detail));

        when(orderRepository.findByOrderIdAndUserUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, userId, false);

        assertEquals(orderId, response.getOrderId());
        assertEquals(1, response.getOrderDetails().size());
        assertEquals("Clean Code", response.getOrderDetails().get(0).getTitle());
    }

    @Test
    void getOrderByIdUsesDiscountedPriceForSubtotal() {
        Long userId = 1L;
        Long orderId = 20L;

        Address address = Address.builder().addressId(5L).build();
        Book book = Book.builder()
                .bookId(7L)
                .title("Effective Java")
                .price(new BigDecimal("100.00"))
                .discountPercent(20)
                .build();
        OrderDetail detail = OrderDetail.builder()
                .book(book)
                .quantity(3)
                .unitPrice(new BigDecimal("80.00"))
                .subtotal(new BigDecimal("240.00"))
                .build();

        Order order = Order.builder()
                .orderId(orderId)
                .address(address)
                .status("PENDING")
                .totalAmount(new BigDecimal("240.00"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .build();
        order.setOrderDetails(List.of(detail));

        when(orderRepository.findByOrderIdAndUserUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId, userId, false);

        assertEquals(new BigDecimal("240.00"), response.getItemsTotal());
        assertEquals(new BigDecimal("80.00"), response.getOrderDetails().get(0).getUnitPrice());
    }
}
