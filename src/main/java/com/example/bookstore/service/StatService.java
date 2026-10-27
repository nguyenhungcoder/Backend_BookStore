package com.example.bookstore.service;

import com.example.bookstore.dto.RevenueByDayResponse;
import com.example.bookstore.dto.RevenueByMonthResponse;
import com.example.bookstore.dto.StatsSummaryResponse;
import com.example.bookstore.entity.Payment;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.PaymentRepository;
import com.example.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public StatService(OrderRepository orderRepository, PaymentRepository paymentRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StatsSummaryResponse getSummary() {
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(this::isSuccessfulPayment)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orderRepository.count();

        long totalCustomers = userRepository.findAll().stream()
                .filter(user -> !"ADMIN".equalsIgnoreCase(user.getRole()))
                .count();

        return StatsSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RevenueByMonthResponse> getRevenueByMonth() {
        List<RevenueByMonthResponse> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();

        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            BigDecimal monthRevenue = paymentRepository.findAll().stream()
                    .filter(this::isSuccessfulPayment)
                    .filter(payment -> payment.getPaidAt() != null
                            && payment.getPaidAt().getYear() == currentYear
                            && payment.getPaidAt().getMonthValue() == currentMonth)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            results.add(RevenueByMonthResponse.builder()
                    .month(month)
                    .monthName(monthNames[month - 1])
                    .revenue(monthRevenue)
                    .build());
        }

        return results;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate swap = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = swap;
        }

        LocalDateTime startDateTime = effectiveStart.atStartOfDay();
        LocalDateTime endDateTime = effectiveEnd.plusDays(1).atStartOfDay();

        BigDecimal revenue = orderRepository.sumPaidRevenueBetween(startDateTime, endDateTime);
        return revenue == null ? BigDecimal.ZERO : revenue;
    }

    @Transactional(readOnly = true)
    public List<RevenueByDayResponse> getRevenueLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        List<Object[]> rawResults = orderRepository.findDailyRevenueSince(startDateTime);
        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        for (Object[] row : rawResults) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.time.LocalDate) {
                date = (LocalDate) row[0];
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            BigDecimal revenue = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            revenueByDate.put(date, revenue);
        }

        List<RevenueByDayResponse> results = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            results.add(RevenueByDayResponse.builder()
                    .date(date.toString())
                    .revenue(revenueByDate.getOrDefault(date, BigDecimal.ZERO))
                    .build());
        }

        return results;
    }

    private boolean isSuccessfulPayment(Payment payment) {
        String status = payment.getStatus();
        return "PAID".equalsIgnoreCase(status)
                || "SUCCESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "SUCCESSFUL".equalsIgnoreCase(status);
    }
}
