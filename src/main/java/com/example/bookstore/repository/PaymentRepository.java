package com.example.bookstore.repository;

import com.example.bookstore.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByOrderOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByTransactionCode(String transactionCode);
}
