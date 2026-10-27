package com.example.bookstore.repository;

import com.example.bookstore.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Review> findByUserUserIdAndBookBookId(Long userId, Long bookId);

    @Query("""
            select r from Review r
            where r.status != 'DELETED'
              and (:keyword is null or lower(r.user.username) like lower(concat('%', :keyword, '%'))
                   or lower(r.book.title) like lower(concat('%', :keyword, '%')))
            """)
    Page<Review> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select r from Review r where r.status != 'DELETED' and r.reviewId = :id")
    Optional<Review> findByIdAndNotDeleted(@Param("id") Long id);

    @Query("""
            select r from Review r
            where r.book.bookId = :bookId and r.status = 'APPROVED'
            order by r.createdAt desc
            """)
    Page<Review> findApprovedReviewsByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("""
            select coalesce(avg(r.rating), 0.0)
            from Review r
            where r.book.bookId = :bookId and r.status = 'APPROVED'
            """)
    Double getAverageRatingByBookId(@Param("bookId") Long bookId);

    @Query("""
            select count(r)
            from Review r
            where r.book.bookId = :bookId and r.status = 'APPROVED'
            """)
    Long countApprovedReviewsByBookId(@Param("bookId") Long bookId);
}

