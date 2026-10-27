package com.example.bookstore.repository;

import com.example.bookstore.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @Query("""
            select a from Author a
            where a.status != 'DELETED'
              and (:keyword is null or lower(a.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Author> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select a from Author a where a.status != 'DELETED' and a.authorId = :id")
    Author findByIdAndNotDeleted(@Param("id") Long id);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, String status);
}

