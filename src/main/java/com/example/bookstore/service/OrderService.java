package com.example.bookstore.service;

import com.example.bookstore.dto.AdminOrderResponse;
import com.example.bookstore.dto.AddressResponse;
import com.example.bookstore.dto.CreateOrderRequest;
import com.example.bookstore.dto.OrderDetailResponse;
import com.example.bookstore.dto.OrderResponse;
import com.example.bookstore.dto.UpdateOrderStatusRequest;
import com.example.bookstore.dto.UserResponse;
import com.example.bookstore.entity.Address;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderDetail;
import com.example.bookstore.entity.User;
import com.example.bookstore.entity.Voucher;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.exception.VoucherInvalidException;
import com.example.bookstore.repository.AddressRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OrderService {

    private static final String ORDER_STATUS_PENDING = "PENDING";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final BookRepository bookRepository;
    private final VoucherService voucherService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            AddressRepository addressRepository,
            BookRepository bookRepository,
            VoucherService voucherService
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.bookRepository = bookRepository;
        this.voucherService = voucherService;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Address address = addressRepository.findByAddressIdAndUserUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        Cart cart = cartRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        List<CartItem> cartItems = List.copyOf(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal itemsTotal = calculateCartItemsTotal(cartItems);
        Voucher voucher = resolveVoucher(request.getVoucherCode(), itemsTotal);
        BigDecimal discountAmount = calculateDiscount(voucher, itemsTotal);
        BigDecimal shippingFee = request.getShippingFee() == null ? BigDecimal.ZERO : request.getShippingFee();
        BigDecimal totalAmount = itemsTotal.subtract(discountAmount).add(shippingFee);
        LocalDateTime now = LocalDateTime.now();
        String paymentMethod = (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank())
                ? "COD"
                : request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        Order order = Order.builder()
                .user(user)
                .address(address)
                .voucher(voucher)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .status(ORDER_STATUS_PENDING)
                .paymentMethod(paymentMethod)
                .createdAt(now)
                .updatedAt(now)
                .build();

       for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();

            validateStock(book, cartItem.getQuantity());

            BigDecimal actualPrice = calculateActualPrice(book);
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(actualPrice)
                    .subtotal(actualPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            order.getOrderDetails().add(orderDetail);
        }

        if (voucher != null) {
            voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
        }

        cart.getItems().clear();

        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, itemsTotal);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> toOrderResponse(order, calculateOrderDetailsTotal(order.getOrderDetails())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId, boolean isAdmin) {
        Order order = isAdmin
                ? orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"))
                : orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return toOrderResponse(order, calculateOrderDetailsTotal(order.getOrderDetails()));
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getAllOrders(String keyword, Pageable pageable) {
        // Gọi hàm findAllActive và truyền keyword xuống cho nó dò tìm
        return orderRepository.findAllActive(keyword, pageable)
                .map(this::toAdminOrderResponse);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, boolean isAdmin) {
        Order order = isAdmin
                ? orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"))
                : orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!ORDER_STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Only pending orders can be cancelled");
        }

        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        return toOrderResponse(updatedOrder, calculateOrderDetailsTotal(updatedOrder.getOrderDetails()));
    }

   @Transactional
    public AdminOrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdAndNotDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // 1. Lấy trạng thái cũ và mới để so sánh
        String oldStatus = order.getStatus();
        String newStatus = request.getStatus().toUpperCase();

        // 2. Logic TRỪ KHO & TĂNG ĐÃ BÁN: Chỉ xảy ra 1 lần duy nhất khi PENDING -> CONFIRMED
        if ("PENDING".equals(oldStatus) && "CONFIRMED".equals(newStatus)) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Book book = detail.getBook();
                
                // Kiểm tra xem kho còn đủ sách không
                if (book.getStockQuantity() < detail.getQuantity()) {
                    throw new RuntimeException("Sách '" + book.getTitle() + "' không đủ số lượng trong kho!");
                }
                
                // Trừ tồn kho và cộng vào đã bán
                book.setStockQuantity(book.getStockQuantity() - detail.getQuantity());
                book.setSold(book.getSold() + detail.getQuantity());
                
                // Lưu lại thay đổi của cuốn sách
                bookRepository.save(book);
            }
        }

        // 3. Logic HOÀN KHO: Khi đơn hàng đang xử lý mà bị HỦY (CANCELLED)
        if (("CONFIRMED".equals(oldStatus) || "PACKING".equals(oldStatus) || "SHIPPING".equals(oldStatus)) 
             && "CANCELLED".equals(newStatus)) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Book book = detail.getBook();
                
                // Cộng trả lại kho và trừ đi số đã bán
                book.setStockQuantity(book.getStockQuantity() + detail.getQuantity());
                book.setSold(book.getSold() - detail.getQuantity());
                
                bookRepository.save(book);
            }
        }

        // 4. Cập nhật thông tin đơn hàng
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // ĐÃ XÓA SẠCH ĐOẠN GỌI HÀM CŨ GÂY LỖI CỘNG DỒN Ở ĐÂY RỒI NHÉ! 🚀

        Order updatedOrder = orderRepository.save(order);
        return toAdminOrderResponse(updatedOrder);
    }
    private boolean isCompletedStatus(String status) {
        return "CONFIRMED".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "PAID".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status);
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

    private BigDecimal calculateCartItemsTotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> calculateActualPrice(item.getBook()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOrderDetailsTotal(List<OrderDetail> orderDetails) {
        return orderDetails.stream()
                .map(detail -> detail.getUnitPrice() == null ? calculateActualPrice(detail.getBook()).multiply(BigDecimal.valueOf(detail.getQuantity())) : detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateActualPrice(Book book) {
        int discountPercent = book.getDiscountPercent() == null ? 0 : book.getDiscountPercent();
        if (discountPercent <= 0) {
            return book.getPrice();
        }

        BigDecimal discountRate = BigDecimal.valueOf(discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return book.getPrice().multiply(BigDecimal.ONE.subtract(discountRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Voucher resolveVoucher(String voucherCode, BigDecimal itemsTotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherService.validateVoucher(voucherCode.trim(), itemsTotal);
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal itemsTotal) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        String discountType = voucher.getDiscountType();
        if ("PERCENT".equalsIgnoreCase(discountType) || "PERCENTAGE".equalsIgnoreCase(discountType)) {
            discount = itemsTotal.multiply(voucher.getDiscountValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else if ("FIXED".equalsIgnoreCase(discountType) || "AMOUNT".equalsIgnoreCase(discountType)) {
            discount = voucher.getDiscountValue();
        } else {
            throw new VoucherInvalidException("Unsupported voucher discount type");
        }

        if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discount = voucher.getMaxDiscountAmount();
        }
        if (discount.compareTo(itemsTotal) > 0) {
            discount = itemsTotal;
        }
        return discount;
    }

    // private void validateAndDecreaseStock(Book book, int quantity) {
    //     if (book.getStockQuantity() < quantity) {
    //         throw new BadRequestException("Not enough stock for book: " + book.getTitle());
    //     }
    //     book.setStockQuantity(book.getStockQuantity() - quantity);
    // }

    // Sửa lại tên hàm và XÓA dòng trừ kho đi
    private void validateStock(Book book, int quantity) {
        if (book.getStockQuantity() < quantity) {
            throw new BadRequestException("Not enough stock for book: " + book.getTitle());
        }
        // Đã xóa dòng: book.setStockQuantity(book.getStockQuantity() - quantity); 
        // -> PENDING sẽ KHÔNG trừ kho nữa!
    }

    private OrderResponse toOrderResponse(Order order, BigDecimal itemsTotal) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .addressId(order.getAddress().getAddressId())
                .voucherCode(order.getVoucher() == null ? null : order.getVoucher().getCode())
                .itemsTotal(itemsTotal)
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .orderDetails(order.getOrderDetails().stream().map(this::toOrderDetailResponse).toList())
                .build();
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        return AdminOrderResponse.builder()
                .orderId(order.getOrderId())
                .user(toUserResponse(order.getUser()))
                .address(toAddressResponse(order.getAddress()))
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .receiverName(address.getReceiverName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.getIsDefault())
                .build();
    }

    private OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail) {
        Book book = orderDetail.getBook();
        return OrderDetailResponse.builder()
                .detailId(orderDetail.getDetailId())
                .bookId(book.getBookId())
                .title(book.getTitle())
                .coverImage(book.getCoverImage())
                .quantity(orderDetail.getQuantity())
                .paymentMethod(orderDetail.getOrder() != null ? orderDetail.getOrder().getPaymentMethod() : null)
                .unitPrice(orderDetail.getUnitPrice())
                .subtotal(orderDetail.getSubtotal())
                .build();
    }
}
