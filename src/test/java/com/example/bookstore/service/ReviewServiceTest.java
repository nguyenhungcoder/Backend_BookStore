package com.example.bookstore.service;

import com.example.bookstore.dto.RatingStatsDTO;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.ReviewRepository;
import com.example.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void getBookReviewStatsReturnsAverageAndCounts() {
        Long bookId = 7L;
        when(reviewRepository.getAverageRatingByBookId(bookId)).thenReturn(4.7);
        when(reviewRepository.countApprovedReviewsByBookId(bookId)).thenReturn(3L);

        RatingStatsDTO response = reviewService.getBookReviewStats(bookId);

        assertEquals(4.7, response.getAverageRating());
        assertEquals(3L, response.getTotalReviews());
    }
}
