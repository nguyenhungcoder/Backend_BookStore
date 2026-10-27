package com.example.bookstore.repository;

import com.example.bookstore.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findAllByStatusIgnoreCaseOrderByStartDateDesc(String status);

    @Query("""
            select v from Voucher v
            where v.status != 'DELETED'
              and (:keyword is null or lower(v.code) like lower(concat('%', :keyword, '%')))
            """)
    Page<Voucher> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select v from Voucher v where v.status != 'DELETED' and v.voucherId = :id")
    Optional<Voucher> findByIdAndNotDeleted(@Param("id") Long id);
}

