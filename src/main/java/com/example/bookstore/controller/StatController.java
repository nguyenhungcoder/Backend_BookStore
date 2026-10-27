package com.example.bookstore.controller;

import com.example.bookstore.dto.RevenueByDayResponse;
import com.example.bookstore.dto.RevenueByMonthResponse;
import com.example.bookstore.dto.StatsSummaryResponse;
import com.example.bookstore.service.StatService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/summary")
    public ResponseEntity<StatsSummaryResponse> getSummary() {
        return ResponseEntity.ok(statService.getSummary());
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<List<RevenueByMonthResponse>> getRevenueByMonth() {
        return ResponseEntity.ok(statService.getRevenueByMonth());
    }

    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> getRevenueByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statService.getRevenueByDateRange(startDate, endDate));
    }

    @GetMapping("/revenue/last-7-days")
    public ResponseEntity<List<RevenueByDayResponse>> getRevenueLast7Days() {
        return ResponseEntity.ok(statService.getRevenueLast7Days());
    }
}
