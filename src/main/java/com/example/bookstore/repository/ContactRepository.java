package com.example.bookstore.repository;

import com.example.bookstore.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("""
            select c from Contact c
            where c.status != 'DELETED'
              and (:keyword is null or lower(c.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(c.email) like lower(concat('%', :keyword, '%')))
            """)
    Page<Contact> findAllActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select c from Contact c where c.status != 'DELETED' and c.contactId = :id")
    Optional<Contact> findByIdAndNotDeleted(@Param("id") Long id);
}

