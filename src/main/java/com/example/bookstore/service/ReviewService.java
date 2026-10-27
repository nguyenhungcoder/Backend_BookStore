package com.example.bookstore.service;

import com.example.bookstore.dto.AdminReviewResponse;
import com.example.bookstore.dto.CreateReviewRequest;
import com.example.bookstore.dto.RatingStatsDTO;
import com.example.bookstore.dto.ReviewRequestDTO;
import com.example.bookstore.dto.ReviewResponseDTO;
import com.example.bookstore.dto.UpdateReviewStatusRequest;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Review;
import com.example.bookstore.entity.User;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.ReviewRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, BookRepository bookRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ReviewResponseDTO createReview(Long userId, ReviewRequestDTO request) {
        validateRating(request.getRating());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (reviewRepository.findByUserUserIdAndBookBookId(userId, book.getBookId()).isPresent()) {
            throw new BadRequestException("You already reviewed this book");
        }

        boolean hasPurchased = orderRepository.findAll().stream()
                .filter(order -> userId.equals(order.getUser().getUserId()))
                .filter(order -> "CONFIRMED".equalsIgnoreCase(order.getStatus()) || "PAID".equalsIgnoreCase(order.getStatus()) || "DELIVERED".equalsIgnoreCase(order.getStatus()))
                .anyMatch(order -> order.getOrderDetails().stream().anyMatch(detail -> detail.getBook().getBookId().equals(book.getBookId())));

        if (!hasPurchased) {
            throw new BadRequestException("Only users who purchased this book can review it");
        }

        LocalDateTime now = LocalDateTime.now();
        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(request.getRating())
                .comment(request.getComment())
                .status("APPROVED")
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponseDTO createReview(Long userId, CreateReviewRequest request) {
        ReviewRequestDTO reviewRequest = new ReviewRequestDTO();
        reviewRequest.setBookId(request.getBookId());
        reviewRequest.setRating(request.getRating());
        reviewRequest.setComment(request.getComment());
        return createReview(userId, reviewRequest);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByBookId(Long bookId, Pageable pageable) {
        return reviewRepository.findApprovedReviewsByBookId(bookId, pageable)
                .map(this::toReviewResponse);
    }

    @Transactional(readOnly = true)
    public RatingStatsDTO getBookReviewStats(Long bookId) {
        Double averageRating = reviewRepository.getAverageRatingByBookId(bookId);
        Long totalReviews = reviewRepository.countApprovedReviewsByBookId(bookId);

        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (totalReviews == null) {
            totalReviews = 0L;
        }

        return RatingStatsDTO.builder()
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .build();
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAllReviews(String keyword, Pageable pageable) {
        return reviewRepository.findAllActive(keyword, pageable)
                .map(this::toAdminReviewResponse);
    }

    @Transactional
    public AdminReviewResponse updateReviewStatus(Long reviewId, UpdateReviewStatusRequest request) {
        Review review = reviewRepository.findByIdAndNotDeleted(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setStatus(request.getStatus());
        review.setUpdatedAt(LocalDateTime.now());

        Review updatedReview = reviewRepository.save(review);
        return toAdminReviewResponse(updatedReview);
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private ReviewResponseDTO toReviewResponse(Review review) {
        return ReviewResponseDTO.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .username(review.getUser().getUsername())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private AdminReviewResponse toAdminReviewResponse(Review review) {
        return AdminReviewResponse.builder()
                .reviewId(review.getReviewId())
                .userId(review.getUser().getUserId())
                .username(review.getUser().getUsername())
                .bookId(review.getBook().getBookId())
                .bookTitle(review.getBook().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
