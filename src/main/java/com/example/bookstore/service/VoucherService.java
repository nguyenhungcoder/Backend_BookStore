package com.example.bookstore.service;

import com.example.bookstore.dto.CreateVoucherRequest;
import com.example.bookstore.dto.UpdateVoucherRequest;
import com.example.bookstore.dto.VoucherResponse;
import com.example.bookstore.entity.Voucher;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.exception.VoucherInvalidException;
import com.example.bookstore.repository.VoucherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> getVouchers() {
        return voucherRepository.findAllByStatusIgnoreCaseOrderByStartDateDesc(ACTIVE_STATUS)
                .stream()
                .map(this::toVoucherResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchersForAdmin(String keyword, Pageable pageable) {
        return voucherRepository.findAllActive(keyword, pageable)
                .map(this::toVoucherResponse);
    }

    @Transactional(readOnly = true)
    public Voucher validateVoucher(String code, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new VoucherInvalidException("Voucher does not exist"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(voucher.getStatus())) {
            throw new VoucherInvalidException("Voucher is inactive");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new VoucherInvalidException("Voucher is not active yet");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new VoucherInvalidException("Voucher has expired");
        }

        Integer usageLimit = voucher.getUsageLimit();
        Integer usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
        if (usageLimit != null && usedCount >= usageLimit) {
            throw new VoucherInvalidException("Voucher usage limit has been reached");
        }

        BigDecimal minOrderValue = voucher.getMinOrderValue();
        if (minOrderValue != null && orderTotal.compareTo(minOrderValue) < 0) {
            throw new VoucherInvalidException("Order total does not meet voucher minimum value");
        }

        return voucher;
    }

    // Admin methods
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        // Check if code already exists
        if (voucherRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new BadRequestException("Voucher code already exists");
        }

        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null 
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date must be before end date");
        }

        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);
        return toVoucherResponse(savedVoucher);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long voucherId, UpdateVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // Check if new code conflicts with existing codes
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(voucher.getCode())) {
            if (voucherRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
                throw new BadRequestException("Voucher code already exists");
            }
            voucher.setCode(request.getCode().toUpperCase());
        }

        if (request.getDescription() != null) {
            voucher.setDescription(request.getDescription());
        }
        if (request.getDiscountType() != null) {
            voucher.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            voucher.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMinOrderValue() != null) {
            voucher.setMinOrderValue(request.getMinOrderValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getUsageLimit() != null) {
            voucher.setUsageLimit(request.getUsageLimit());
        }
        if (request.getStartDate() != null) {
            voucher.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            voucher.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            voucher.setStatus(request.getStatus());
        }

        Voucher updatedVoucher = voucherRepository.save(voucher);
        return toVoucherResponse(updatedVoucher);
    }

    @Transactional
    public void deleteVoucher(Long voucherId) {
        Voucher voucher = voucherRepository.findByIdAndNotDeleted(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        
        // Soft delete: set status to DELETED
        voucher.setStatus("DELETED");
        voucherRepository.save(voucher);
    }

    private VoucherResponse toVoucherResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .status(voucher.getStatus())
                .build();
    }
}
