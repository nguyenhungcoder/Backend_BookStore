package com.example.bookstore.repository;

import com.example.bookstore.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("""
            select b from Banner b
            where b.status != 'DELETED'
            """)
    Page<Banner> findAllActive(Pageable pageable);

    @Query("select b from Banner b where b.status != 'DELETED' and b.bannerId = :id")
    Optional<Banner> findByIdAndNotDeleted(@Param("id") Long id);
}

