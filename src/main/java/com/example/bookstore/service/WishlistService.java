package com.example.bookstore.service;

import com.example.bookstore.dto.WishlistResponseDTO;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.User;
import com.example.bookstore.entity.Wishlist;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public WishlistService(WishlistRepository wishlistRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public Wishlist addToWishlist(Long userId, Long bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (wishlistRepository.findByUserUserIdAndBookBookId(userId, bookId).isPresent()) {
            throw new BadRequestException("Book is already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .book(book)
                .createdAt(LocalDateTime.now())
                .build();
        return wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long bookId) {
        Wishlist wishlist = wishlistRepository.findByUserUserIdAndBookBookId(userId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }

  public List<WishlistResponseDTO> getWishlistBooks(Long userId) {
        return wishlistRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(wishlist -> {
                    Book book = wishlist.getBook();
                    
                    // An toàn lôi tên tác giả ra, tránh lỗi rỗng
                    String authorNames = "Đang cập nhật";
                    if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                        authorNames = book.getAuthors().stream()
                                .map(author -> author.getName()) // Lấy tên tác giả ghép lại
                                .collect(java.util.stream.Collectors.joining(", "));
                    }

                    // Bỏ sách vào vỏ hộp DTO để cắt đứt vòng lặp
                    return WishlistResponseDTO.builder()
                            .id(book.getBookId())
                            .bookId(book.getBookId())
                            .title(book.getTitle())
                            .coverImage(book.getCoverImage())
                            .price(book.getPrice())
                            .author(authorNames)
                            // ❤️ THÊM DÒNG NÀY LÀ MÔ TẢ SẼ HIỆN LÊN GIAO DIỆN NGAY:
                            .description(book.getDescription())
                            .build();
                })
                .toList();
    }


    
}
