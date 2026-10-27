package com.example.bookstore.repository;

import com.example.bookstore.entity.Wishlist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

// ❤️ THÊM DÒNG @EntityGraph NÀY ĐỂ TRỊ TẬN GỐC BỆNH LƯỜI (LAZY LOAD):
    @EntityGraph(attributePaths = {"book", "book.authors", "book.categories", "book.publisher"})
    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(Long userId);


    Optional<Wishlist> findByUserUserIdAndBookBookId(Long userId, Long bookId);
}
