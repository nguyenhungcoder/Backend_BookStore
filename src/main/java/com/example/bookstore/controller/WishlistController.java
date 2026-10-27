package com.example.bookstore.controller;

import com.example.bookstore.dto.WishlistResponseDTO;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Wishlist;
import com.example.bookstore.security.UserPrincipal;
import com.example.bookstore.service.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    public ResponseEntity<?> addToWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long bookId
    ) {
        // Thực hiện thêm vào yêu thích
        wishlistService.addToWishlist(principal.getUserId(), bookId);
        
        // Chỉ trả về chữ "Success" hoặc mã 200 OK thay vì trả về cả Entity
        return ResponseEntity.ok().body("Thêm vào yêu thích thành công!");
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long bookId
    ) {
        wishlistService.removeFromWishlist(principal.getUserId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponseDTO>> getWishlistBooks(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(wishlistService.getWishlistBooks(principal.getUserId()));
    }
}
