package com.example.bookstore.service;

import com.example.bookstore.dto.AddCartItemRequest;
import com.example.bookstore.dto.CartItemResponse;
import com.example.bookstore.dto.UpdateCartItemRequest;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.entity.User;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartItemRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            BookRepository bookRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(Long userId) {
        Cart cart = cartRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return List.of();
        }

        return cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CartItemResponse addItem(Long userId, AddCartItemRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Cart cart = getOrCreateCart(userId);
        CartItem cartItem = cartItemRepository.findByCartCartIdAndBookBookId(cart.getCartId(), book.getBookId())
                .orElse(null);

        int requestedQuantity = request.getQuantity();
        int finalQuantity = requestedQuantity + (cartItem == null ? 0 : cartItem.getQuantity());
        validateStock(book, finalQuantity);

        if (cartItem == null) {
            cartItem = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(requestedQuantity)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            cartItem.setQuantity(finalQuantity);
        }

        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartItemResponse updateItemQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem cartItem = getOwnedCartItem(userId, itemId);
        validateStock(cartItem.getBook(), request.getQuantity());
        cartItem.setQuantity(request.getQuantity());
        return toCartItemResponse(cartItem);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        CartItem cartItem = getOwnedCartItem(userId, itemId);
        cartItemRepository.delete(cartItem);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart cart = Cart.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(cart);
                });
    }

    private CartItem getOwnedCartItem(Long userId, Long itemId) {
        return cartItemRepository.findByItemIdAndCartUserUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    }

    private void validateStock(Book book, int quantity) {
        if (quantity > book.getStockQuantity()) {
            throw new BadRequestException("Not enough stock for book: " + book.getTitle());
        }
    }

    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        Book book = cartItem.getBook();
        BigDecimal actualPrice = calculateActualPrice(book);
        BigDecimal subtotal = actualPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        String author = book.getAuthors() == null || book.getAuthors().isEmpty()
                ? "Chưa cập nhật"
                : book.getAuthors().stream()
                .map(authorEntity -> authorEntity.getName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));

        String category = book.getCategories() == null || book.getCategories().isEmpty()
                ? "Chưa cập nhật"
                : book.getCategories().stream()
                .map(categoryEntity -> categoryEntity.getName())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));

        String publisher = book.getPublisher() == null || book.getPublisher().getName() == null || book.getPublisher().getName().isBlank()
                ? "Chưa cập nhật"
                : book.getPublisher().getName();

        return CartItemResponse.builder()
                .itemId(cartItem.getItemId())
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(author)
                .category(category)
                .publisher(publisher)
                .coverImage(book.getCoverImage())
                .price(actualPrice)
                .stockQuantity(book.getStockQuantity())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .createdAt(cartItem.getCreatedAt())
                .build();
    }

    private BigDecimal calculateActualPrice(Book book) {
        int discountPercent = book.getDiscountPercent() == null ? 0 : book.getDiscountPercent();
        if (discountPercent <= 0) {
            return book.getPrice();
        }

        BigDecimal discountRate = BigDecimal.valueOf(discountPercent)
                .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        return book.getPrice().multiply(BigDecimal.ONE.subtract(discountRate))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
