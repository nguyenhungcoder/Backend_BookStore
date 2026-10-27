package com.example.bookstore.service;

import com.example.bookstore.dto.AuthorSummaryDTO;
import com.example.bookstore.dto.BookListItemResponseDTO;
import com.example.bookstore.dto.BookResponseDTO;
import com.example.bookstore.dto.CategorySummaryDTO;
import com.example.bookstore.dto.CreateBookRequest;
import com.example.bookstore.dto.PublisherSummaryDTO;
import com.example.bookstore.dto.UploadCoverRequest;
import com.example.bookstore.dto.UpdateBookRequest;
import com.example.bookstore.entity.Author;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Category;
import com.example.bookstore.entity.Publisher;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.AuthorRepository;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.PublisherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final FileUploadService fileUploadService;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository,
                       AuthorRepository authorRepository, PublisherRepository publisherRepository,
                       FileUploadService fileUploadService) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.fileUploadService = fileUploadService;
    }

    @Transactional(readOnly = true)
    public Page<BookListItemResponseDTO> getBooks(Collection<Long> categoryIds, Collection<Long> publisherIds, Collection<Long> authorIds, String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<Long> safeCategoryIds = normalizeIdList(categoryIds);
        List<Long> safePublisherIds = normalizeIdList(publisherIds);
        List<Long> safeAuthorIds = normalizeIdList(authorIds);
        return bookRepository.searchBooks(safeCategoryIds, safePublisherIds, safeAuthorIds, normalizedKeyword, pageable)
                .map(this::toListItemResponse);
    }

    @Transactional(readOnly = true)
    public BookResponseDTO getBookById(Long bookId) {
        Book book = bookRepository.findByBookId(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        return toBookResponse(book);
    }

    // Admin methods
    @Transactional
    public BookResponseDTO createBook(CreateBookRequest request) {
        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));

        LocalDateTime now = LocalDateTime.now();
        Book book = Book.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .coverImage(request.getCoverImage())
                .isbn(request.getIsbn())
                .publicationYear(request.getPublicationYear())
                .language(request.getLanguage())
                .pageCount(request.getPageCount())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .price(request.getPrice())
                .discountPercent(request.getDiscountPercent() == null ? 0 : request.getDiscountPercent())
                .stockQuantity(request.getStockQuantity())
                .sold(0)
                .status(request.getStatus())
                .publisher(publisher)
                .authors(new HashSet<>())
                .categories(new HashSet<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Add categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            book.setCategories(categories);
        }

        // Add authors
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            Set<Author> authors = new HashSet<>(authorRepository.findAllById(request.getAuthorIds()));
            book.setAuthors(authors);
        }

        Book savedBook = bookRepository.save(book);
        return toBookResponse(savedBook);
    }

    @Transactional
    public BookResponseDTO updateBook(Long bookId, UpdateBookRequest request) {
        Book book = bookRepository.findByBookId(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        // Update basic fields if provided
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getCoverImage() != null) {
            book.setCoverImage(request.getCoverImage());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getPublicationYear() != null) {
            book.setPublicationYear(request.getPublicationYear());
        }
        if (request.getLanguage() != null) {
            book.setLanguage(request.getLanguage());
        }
        if (request.getPageCount() != null) {
            book.setPageCount(request.getPageCount());
        }
        if (request.getWeight() != null) {
            book.setWeight(request.getWeight());
        }
        if (request.getDimensions() != null) {
            book.setDimensions(request.getDimensions());
        }
        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }
        if (request.getDiscountPercent() != null) {
            book.setDiscountPercent(request.getDiscountPercent());
        }
        if (request.getStockQuantity() != null) {
            book.setStockQuantity(request.getStockQuantity());
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }

        // Update publisher if provided
        if (request.getPublisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
            book.setPublisher(publisher);
        }

        // Update categories if provided
        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            book.setCategories(categories);
        }

        // Update authors if provided
        if (request.getAuthorIds() != null) {
            Set<Author> authors = new HashSet<>(authorRepository.findAllById(request.getAuthorIds()));
            book.setAuthors(authors);
        }

        book.setUpdatedAt(LocalDateTime.now());
        Book updatedBook = bookRepository.save(book);
        return toBookResponse(updatedBook);
    }

    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findByBookId(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        
        // Soft delete: set status to DELETED
        book.setStatus("DELETED");
        bookRepository.save(book);
    }

    @Transactional
    public BookResponseDTO uploadBookCover(Long bookId, UploadCoverRequest request) {
        Book book = bookRepository.findByBookId(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        String imagePath = fileUploadService.uploadBase64File(request.getCoverImage(), request.getFileName(), "books");
        book.setCoverImage(imagePath);
        Book updatedBook = bookRepository.save(book);
        return toBookResponse(updatedBook);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private List<Long> normalizeIdList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private BookListItemResponseDTO toListItemResponse(Book book) {
        return BookListItemResponseDTO.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .coverImage(book.getCoverImage())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .publicationYear(book.getPublicationYear())
                .price(book.getPrice())
                .discountPercent(book.getDiscountPercent())
                .stockQuantity(book.getStockQuantity())
                .sold(book.getSold())
                .status(book.getStatus())
                .publisher(toPublisherSummary(book.getPublisher()))
                .categories(book.getCategories().stream().map(this::toCategorySummary).toList())
                .authors(book.getAuthors().stream().map(this::toAuthorSummary).toList())
                .createdAt(book.getCreatedAt())
                .build();
    }

    private BookResponseDTO toBookResponse(Book book) {
        return BookResponseDTO.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .description(book.getDescription())
                .coverImage(book.getCoverImage())
                .isbn(book.getIsbn())
                .publicationYear(book.getPublicationYear())
                .language(book.getLanguage())
                .pageCount(book.getPageCount())
                .weight(book.getWeight())
                .dimensions(book.getDimensions())
                .price(book.getPrice())
                .discountPercent(book.getDiscountPercent())
                .stockQuantity(book.getStockQuantity())
                .sold(book.getSold())
                .status(book.getStatus())
                .publisher(toPublisherSummary(book.getPublisher()))
                .authors(book.getAuthors().stream().map(this::toAuthorSummary).toList())
                .categories(book.getCategories().stream().map(this::toCategorySummary).toList())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    private PublisherSummaryDTO toPublisherSummary(Publisher publisher) {
        if (publisher == null) {
            return null;
        }
        return PublisherSummaryDTO.builder()
                .publisherId(publisher.getPublisherId())
                .name(publisher.getName())
                .address(publisher.getAddress())
                .phone(publisher.getPhone())
                .website(publisher.getWebsite())
                .build();
    }

    private AuthorSummaryDTO toAuthorSummary(Author author) {
        return AuthorSummaryDTO.builder()
                .authorId(author.getAuthorId())
                .name(author.getName())
                .bio(author.getBio())
                .avatar(author.getAvatar())
                .build();
    }

    private CategorySummaryDTO toCategorySummary(Category category) {
        return CategorySummaryDTO.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .build();
    }
}
