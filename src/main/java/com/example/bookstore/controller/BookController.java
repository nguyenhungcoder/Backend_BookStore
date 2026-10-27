package com.example.bookstore.controller;

import com.example.bookstore.dto.BookListItemResponseDTO;
import com.example.bookstore.dto.BookResponseDTO;
import com.example.bookstore.dto.CreateBookRequest;
import com.example.bookstore.dto.UploadCoverRequest;
import com.example.bookstore.dto.UpdateBookRequest;
import com.example.bookstore.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<Page<BookListItemResponseDTO>> getBooks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryIds,
            @RequestParam(required = false) List<Long> category,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String publisherIds,
            @RequestParam(required = false) List<Long> publisher,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String authorIds,
            @RequestParam(required = false) List<Long> author,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String title,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String searchKeyword = keyword != null ? keyword : title;
        List<Long> normalizedCategoryIds = normalizeIdList(categoryIds, categoryId, category);
        List<Long> normalizedPublisherIds = normalizeIdList(publisherIds, publisherId, publisher);
        List<Long> normalizedAuthorIds = normalizeIdList(authorIds, authorId, author);
        Pageable safePageable = sanitizePageable(pageable);
        return ResponseEntity.ok(bookService.getBooks(normalizedCategoryIds, normalizedPublisherIds, normalizedAuthorIds, searchKeyword, safePageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDTO> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    private Pageable sanitizePageable(Pageable pageable) {
        List<Sort.Order> safeOrders = pageable.getSort().stream()
                .filter(order -> List.of("createdAt", "price", "title", "sold", "id", "bookId").contains(order.getProperty()))
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList();

        if (safeOrders.isEmpty()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(safeOrders));
    }

    private String mapSortProperty(String property) {
        if ("id".equals(property)) {
            return "bookId";
        }
        return property;
    }

    private List<Long> normalizeIdList(String csvValue, Long singleValue, Collection<Long> multiValues) {
        List<Long> ids = new ArrayList<>();
        if (csvValue != null && !csvValue.isBlank()) {
            ids.addAll(Arrays.stream(csvValue.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::parseLong)
                    .toList());
        }
        if (singleValue != null) {
            ids.add(singleValue);
        }
        if (multiValues != null && !multiValues.isEmpty()) {
            ids.addAll(multiValues);
        }
        return ids.isEmpty() ? null : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    // Admin endpoints
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BookResponseDTO> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponseDTO response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BookResponseDTO> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        BookResponseDTO response = bookService.updateBook(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/upload-cover")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<BookResponseDTO> uploadBookCover(
            @PathVariable Long id,
            @Valid @RequestBody UploadCoverRequest request
    ) {
        BookResponseDTO response = bookService.uploadBookCover(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
