package com.example.bookstore.repository;

import com.example.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"cart", "book"})
    Optional<CartItem> findByItemIdAndCartUserUserId(Long itemId, Long userId);

    @EntityGraph(attributePaths = {"cart", "book"})
    Optional<CartItem> findByCartCartIdAndBookBookId(Long cartId, Long bookId);
}
