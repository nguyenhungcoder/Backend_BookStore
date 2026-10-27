package com.example.bookstore.repository;

import com.example.bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    @Query(
            value = """
                    select distinct b
                    from Book b
                    left join b.categories c
                    left join b.authors a
                    where b.status != 'DELETED'
                      and (:categoryIds is null or c.categoryId in :categoryIds)
                      and (:publisherIds is null or b.publisher.publisherId in :publisherIds)
                      and (:authorIds is null or a.authorId in :authorIds)
                      and (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%')) or lower(b.isbn) like lower(concat('%', :keyword, '%')))
                    """,
            countQuery = """
                    select count(distinct b)
                    from Book b
                    left join b.authors a
                    left join b.categories c
                    where b.status != 'DELETED'
                      and (:categoryIds is null or c.categoryId in :categoryIds)
                      and (:publisherIds is null or b.publisher.publisherId in :publisherIds)
                      and (:authorIds is null or a.authorId in :authorIds)
                      and (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%')) or lower(b.isbn) like lower(concat('%', :keyword, '%')))
                    """
    )
    
    Page<Book> searchBooks(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("publisherIds") Collection<Long> publisherIds,
            @Param("authorIds") Collection<Long> authorIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"publisher", "authors", "categories"})
    Optional<Book> findByBookId(Long bookId);
}