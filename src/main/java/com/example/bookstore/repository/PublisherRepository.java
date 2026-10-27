package com.example.bookstore.repository;

import com.example.bookstore.entity.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    @Query("""
            select p from Publisher p
            where p.status != 'DELETED'
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Publisher> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select p from Publisher p where p.status != 'DELETED' and p.publisherId = :id")
    Publisher findByIdAndNotDeleted(@Param("id") Long id);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, String status);
}

