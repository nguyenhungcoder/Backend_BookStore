package com.example.bookstore.repository;

import com.example.bookstore.dto.RevenueByDayResponse;
import com.example.bookstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderIdAndUserUserId(Long orderId, Long userId);

    List<Order> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "address"})
    Page<Order> findAll(Pageable pageable);

    @Query("""
            select o from Order o
            left join o.user u
            where o.status != 'DELETED'
              and (:keyword is null 
                   or concat(o.orderId, '') like concat('%', :keyword, '%')
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%')))
            """)
    Page<Order> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select o from Order o where o.status != 'DELETED' and o.orderId = :id")
    Optional<Order> findByIdAndNotDeleted(@Param("id") Long id);

    @Query(value = "SELECT DATE(o.created_at) AS date, SUM(o.total_amount) AS revenue " +
            "FROM orders o " +
            "WHERE o.status != 'DELETED' " +
            "AND o.created_at >= :startDate " +
            "GROUP BY DATE(o.created_at) " +
            "ORDER BY DATE(o.created_at)", nativeQuery = true)
    List<Object[]> findDailyRevenueSince(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("""
            select coalesce(sum(o.totalAmount), 0)
            from Order o
            where o.status != 'DELETED'
              and o.createdAt >= :startDate
              and o.createdAt < :endDate
              and upper(o.status) in ('PAID', 'CONFIRMED', 'COMPLETED', 'SUCCESS', 'SUCCESSFUL')
            """)
    BigDecimal sumPaidRevenueBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                     @Param("endDate") java.time.LocalDateTime endDate);
}