package com.example.bookstore.controller;

import com.example.bookstore.dto.AdminReviewResponse;
import com.example.bookstore.dto.RatingStatsDTO;
import com.example.bookstore.dto.ReviewRequestDTO;
import com.example.bookstore.dto.ReviewResponseDTO;
import com.example.bookstore.dto.UpdateReviewStatusRequest;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.security.UserPrincipal;
import com.example.bookstore.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        if (principal == null || principal.getUserId() == null) {
            throw new BadRequestException("Authentication required");
        }

        ReviewResponseDTO review = reviewService.createReview(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByBookId(
            @PathVariable Long bookId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId, pageable));
    }

    @GetMapping("/book/{bookId}/stats")
    public ResponseEntity<RatingStatsDTO> getBookReviewStats(@PathVariable Long bookId) {
        return ResponseEntity.ok(reviewService.getBookReviewStats(bookId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<AdminReviewResponse>> getAllReviews(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getAllReviews(keyword, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AdminReviewResponse> updateReviewStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewStatusRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(id, request));
    }
}
