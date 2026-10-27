package com.example.bookstore.service;

import com.example.bookstore.dto.AuthorResponse;
import com.example.bookstore.dto.CreateAuthorRequest;
import com.example.bookstore.dto.UpdateAuthorRequest;
import com.example.bookstore.entity.Author;
import com.example.bookstore.exception.DuplicateResourceException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorResponse> getAllAuthors(Pageable pageable, String keyword) {
        return authorRepository.findAllActive(keyword, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findByIdAndNotDeleted(id);
        if (author == null) {
            throw new ResourceNotFoundException("Author not found");
        }
        return toResponse(author);
    }

    @Override
    @Transactional
    public AuthorResponse createAuthor(CreateAuthorRequest request) {
        if (authorRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Author already exists");
        }

        Author author = Author.builder()
                .name(request.getName())
                .bio(request.getBio())
                .avatar(request.getAvatar())
                .status(request.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional
    public AuthorResponse updateAuthor(Long id, UpdateAuthorRequest request) {
        Author author = authorRepository.findByIdAndNotDeleted(id);
        if (author == null) {
            throw new ResourceNotFoundException("Author not found");
        }

        if (request.getName() != null && !request.getName().equalsIgnoreCase(author.getName())
                && authorRepository.existsByNameIgnoreCaseAndStatusNot(request.getName(), "DELETED")) {
            throw new DuplicateResourceException("Author already exists");
        }

        if (request.getName() != null) author.setName(request.getName());
        if (request.getBio() != null) author.setBio(request.getBio());
        if (request.getAvatar() != null) author.setAvatar(request.getAvatar());
        if (request.getStatus() != null) author.setStatus(request.getStatus());
        author.setUpdatedAt(LocalDateTime.now());

        return toResponse(authorRepository.save(author));
    }

    @Override
    @Transactional
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findByIdAndNotDeleted(id);
        if (author == null) {
            throw new ResourceNotFoundException("Author not found");
        }
        author.setStatus("DELETED");
        author.setUpdatedAt(LocalDateTime.now());
        authorRepository.save(author);
    }

    private AuthorResponse toResponse(Author author) {
        return AuthorResponse.builder()
                .authorId(author.getAuthorId())
                .name(author.getName())
                .bio(author.getBio())
                .avatar(author.getAvatar())
                .status(author.getStatus())
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();
    }
}
