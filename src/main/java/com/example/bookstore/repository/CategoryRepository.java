package com.example.bookstore.repository;

import com.example.bookstore.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select c from Category c
            where c.status != 'DELETED'
              and (:keyword is null or lower(c.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Category> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select c from Category c where c.status != 'DELETED' and c.categoryId = :id")
    Category findByIdAndNotDeleted(@Param("id") Long id);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, String status);
}

